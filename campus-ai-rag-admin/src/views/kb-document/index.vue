<template>
  <div class="kb-document-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">知识库文档管理</h2>
        <p class="toolbar-desc">对齐后端 /admin/kb/documents，支持上传、状态更新、重试、解析与向量化入库</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadDocumentList">刷新</el-button>
        <el-button :disabled="!lastImageTaskId" @click="openLastTaskView">返回上次任务</el-button>
        <el-button type="primary" @click="openUploadDialog">上传文档</el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-wrap" v-loading="tableLoading">
      <el-table :data="documentList" height="100%">
        <el-table-column prop="id" label="ID" min-width="70" />
        <el-table-column prop="title" label="文档标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryId" label="分类ID" min-width="90" />
        <el-table-column prop="originalFileName" label="原始文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileExt" label="后缀" min-width="80" />
        <el-table-column prop="fileSize" label="大小(字节)" min-width="110" />
        <el-table-column label="处理状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.processStatus)">
              {{ row.processStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" min-width="90" />
        <el-table-column prop="chunkCount" label="切片数" min-width="90" />
        <el-table-column prop="failReason" label="失败原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" fixed="right" min-width="330">
          <template #default="{ row }">
            <el-button link type="primary" @click="openUpdateStatusDialog(row)">改状态</el-button>
            <el-button link type="warning" @click="handleRetry(row)">重试</el-button>
            <el-button link type="success" @click="handleParse(row)">解析</el-button>
            <el-button link type="success" @click="handleIndex(row)">向量化</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageState.pageNum"
          v-model:page-size="pageState.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pageState.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="handlePageNumChange"
        />
      </div>
    </div>

    <el-dialog v-model="uploadDialogVisible" title="上传知识库文档" width="760px" destroy-on-close>
      <el-form ref="uploadFormRef" :model="uploadFormData" :rules="uploadFormRules" label-width="120px">
        <el-form-item label="分类ID" prop="categoryId">
          <el-select v-model="uploadFormData.categoryId" class="full-width" filterable>
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="`${item.categoryName} (${item.categoryCode})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="uploadFormData.title" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="uploadFormData.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="选择文件" prop="file">
          <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            :on-change="handleUploadFileChange"
            :on-remove="handleUploadFileRemove"
          >
            <template #trigger>
              <el-button type="primary" plain>选择文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">支持 pdf/txt/md/doc/docx，上传后初始状态为 PENDING</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <div class="image-understand-panel">
        <div class="image-understand-actions">
          <el-button
            :disabled="!uploadedDocumentId || imageTaskRunning"
            :loading="imagePreviewLoading"
            @click="handlePreviewImageUnderstand"
          >
            {{ imagePreviewLoading ? '解析图片中...' : '解析图片' }}
          </el-button>
          <el-button
            v-if="imageUnderstandText"
            type="primary"
            :disabled="imageTaskRunning"
            :loading="imageIndexLoading"
            @click="handleImageUnderstandIndex"
          >
            {{ imageIndexLoading ? '入库中...' : '图片文本一键向量化' }}
          </el-button>
          <div v-if="!uploadedDocumentId" class="image-understand-tip">
            请先“确认上传”生成文档后再解析图片
          </div>
        </div>

        <div v-if="imageTaskList.length" class="image-task-picker">
          <el-select
            v-model="selectedImageTaskId"
            class="full-width"
            filterable
            placeholder="选择某次图片理解任务后再执行一键向量化"
            @change="handleSelectTask"
          >
            <el-option
              v-for="task in imageTaskList"
              :key="task.taskId"
              :label="`${task.taskType}-${task.taskStatus} (${task.taskId.slice(0, 8)})`"
              :value="task.taskId"
            />
          </el-select>
        </div>

        <div v-if="currentImageTask.taskId" class="image-task-status">
          <span>任务ID：{{ currentImageTask.taskId }}</span>
          <span>状态：{{ currentImageTask.taskStatus || '-' }}</span>
          <span v-if="currentImageTask.message">说明：{{ currentImageTask.message }}</span>
        </div>

        <el-input
          v-if="imageUnderstandText"
          v-model="imageUnderstandText"
          type="textarea"
          :rows="6"
          placeholder="图片理解结果（可修改后再一键向量化）"
        />
      </div>

      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button :loading="uploadAndIndexLoading" @click="handleSubmitUploadAndIndex">
          {{ uploadAndIndexLoading ? '处理中...' : '一键解析' }}
        </el-button>
        <el-button type="primary" :loading="uploadLoading" @click="handleSubmitUpload">
          {{ uploadLoading ? '上传中...' : '确认上传' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusDialogVisible" title="更新文档状态" width="680px" destroy-on-close>
      <el-form ref="statusFormRef" :model="statusFormData" :rules="statusFormRules" label-width="120px">
        <el-form-item label="文档标题">
          <el-input v-model="statusFormData.title" disabled />
        </el-form-item>
        <el-form-item label="处理状态" prop="processStatus">
          <el-select v-model="statusFormData.processStatus" class="full-width">
            <el-option label="PENDING" value="PENDING" />
            <el-option label="PROCESSING" value="PROCESSING" />
            <el-option label="SUCCESS" value="SUCCESS" />
            <el-option label="FAILED" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="失败原因" prop="failReason">
          <el-input v-model="statusFormData.failReason" type="textarea" :rows="3" placeholder="状态为 FAILED 时建议填写" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusLoading" @click="handleSubmitStatus">
          {{ statusLoading ? '提交中...' : '确认' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getKbImageUnderstandTaskList,
  getKbImageUnderstandTask,
  getKbCategoryList,
  getKbDocumentList,
  indexKbDocument,
  parseKbDocument,
  retryKbDocument,
  submitKbImageUnderstandIndexTaskByTaskId,
  submitKbImageUnderstandPreviewTask,
  updateKbDocumentStatus,
  uploadAndIndexKbDocument,
  uploadKbDocument
} from '@/api/kb'

const tableLoading = ref(false)
const documentList = ref([])
const pageState = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const categoryOptions = ref([])

const uploadDialogVisible = ref(false)
const uploadLoading = ref(false)
const uploadAndIndexLoading = ref(false)
const uploadFormRef = ref()
const uploadFormData = reactive({
  categoryId: null,
  title: '',
  remark: '',
  file: null
})
const uploadFormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
  file: [{ required: true, message: '请选择文件', trigger: 'change' }]
}

const uploadedDocumentId = ref(null)
const imagePreviewLoading = ref(false)
const imageIndexLoading = ref(false)
const imageUnderstandText = ref('')
const IMAGE_TASK_STORAGE_KEY = 'kb_image_last_task_id'
const lastImageTaskId = ref(localStorage.getItem(IMAGE_TASK_STORAGE_KEY) || '')
const imageTaskPollingTimer = ref(null)
const currentImageTask = reactive({
  taskId: '',
  documentId: null,
  taskType: '',
  taskStatus: '',
  message: '',
  mergedText: ''
})
const imageTaskList = ref([])
const selectedImageTaskId = ref('')
const imageTaskRunning = computed(() =>
  currentImageTask.taskStatus === 'PENDING' || currentImageTask.taskStatus === 'PROCESSING'
)

const statusDialogVisible = ref(false)
const statusLoading = ref(false)
const statusFormRef = ref()
const statusFormData = reactive({
  id: null,
  title: '',
  processStatus: 'PENDING',
  failReason: ''
})
const statusFormRules = {
  processStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const getStatusTag = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PROCESSING') return 'warning'
  return 'info'
}

const patchCurrentImageTask = (task) => {
  currentImageTask.taskId = task?.taskId || ''
  currentImageTask.documentId = task?.documentId ?? null
  currentImageTask.taskType = task?.taskType || ''
  currentImageTask.taskStatus = task?.taskStatus || ''
  currentImageTask.message = task?.message || ''
  currentImageTask.mergedText = task?.mergedText || ''
}

const saveLastTaskId = (taskId) => {
  if (!taskId) {
    lastImageTaskId.value = ''
    localStorage.removeItem(IMAGE_TASK_STORAGE_KEY)
    return
  }
  lastImageTaskId.value = taskId
  localStorage.setItem(IMAGE_TASK_STORAGE_KEY, taskId)
}

const stopImageTaskPolling = () => {
  if (imageTaskPollingTimer.value) {
    clearInterval(imageTaskPollingTimer.value)
    imageTaskPollingTimer.value = null
  }
}

const handleTaskUpdated = async (task, silent = false) => {
  patchCurrentImageTask(task)
  if (task?.documentId) {
    uploadedDocumentId.value = task.documentId
  }

  if (task?.taskType === 'PREVIEW' && task?.taskStatus === 'SUCCESS') {
    imageUnderstandText.value = task?.mergedText || ''
  }

  if (task?.taskStatus === 'SUCCESS' || task?.taskStatus === 'FAILED') {
    stopImageTaskPolling()
    await loadDocumentList()
    await loadImageTaskList(task?.documentId || uploadedDocumentId.value)
    if (!silent) {
      if (task.taskStatus === 'SUCCESS') {
        ElMessage.success(task?.message || '任务处理成功')
      } else {
        ElMessage.error(task?.message || '任务处理失败')
      }
    }
  }
}

const handleSelectTask = async (taskId) => {
  if (!taskId) return
  await queryTaskAndSync(taskId, true)
}

const queryTaskAndSync = async (taskId, silent = false) => {
  if (!taskId) return
  const res = await getKbImageUnderstandTask(taskId)
  await handleTaskUpdated(res.data, silent)
}

const startImageTaskPolling = (taskId) => {
  stopImageTaskPolling()
  if (!taskId) return
  imageTaskPollingTimer.value = setInterval(async () => {
    try {
      await queryTaskAndSync(taskId, true)
    } catch (error) {
      stopImageTaskPolling()
    }
  }, 2000)
}

const loadCategoryOptions = async () => {
  const res = await getKbCategoryList()
  categoryOptions.value = res.data || []
}

const loadDocumentList = async () => {
  tableLoading.value = true
  try {
    const res = await getKbDocumentList({
      pageNum: pageState.pageNum,
      pageSize: pageState.pageSize
    })
    const pageData = res.data || {}
    documentList.value = pageData.list || []
    pageState.total = pageData.total || 0
  } finally {
    tableLoading.value = false
  }
}

const handlePageNumChange = async (value) => {
  pageState.pageNum = value
  await loadDocumentList()
}

const handlePageSizeChange = async (value) => {
  pageState.pageSize = value
  pageState.pageNum = 1
  await loadDocumentList()
}

const loadImageTaskList = async (documentId) => {
  if (!documentId) {
    imageTaskList.value = []
    selectedImageTaskId.value = ''
    return
  }
  const res = await getKbImageUnderstandTaskList({ documentId })
  imageTaskList.value = res.data || []
  if (!selectedImageTaskId.value && imageTaskList.value.length) {
    selectedImageTaskId.value = imageTaskList.value[0].taskId
  }
}

const resetUploadForm = () => {
  uploadFormData.categoryId = null
  uploadFormData.title = ''
  uploadFormData.remark = ''
  uploadFormData.file = null
  imageUnderstandText.value = ''
  patchCurrentImageTask(null)
  imageTaskList.value = []
  selectedImageTaskId.value = ''
}

const openUploadDialog = async () => {
  resetUploadForm()
  await loadCategoryOptions()
  uploadDialogVisible.value = true
}

const openLastTaskView = async () => {
  if (!lastImageTaskId.value) {
    ElMessage.info('暂无上次任务记录')
    return
  }
  await loadCategoryOptions()
  uploadDialogVisible.value = true
  await queryTaskAndSync(lastImageTaskId.value, true)
  await loadImageTaskList(currentImageTask.documentId || uploadedDocumentId.value)
  if (imageTaskRunning.value) {
    startImageTaskPolling(lastImageTaskId.value)
  }
}

const handleUploadFileChange = (file) => {
  uploadFormData.file = file.raw
  if (!uploadFormData.title || !uploadFormData.title.trim()) {
    const name = file?.name || file?.raw?.name || ''
    const baseName = name.includes('.') ? name.slice(0, name.lastIndexOf('.')) : name
    uploadFormData.title = baseName || ''
  }
}

const handleUploadFileRemove = () => {
  uploadFormData.file = null
}

const handleSubmitUpload = async () => {
  const valid = await uploadFormRef.value?.validate().catch(() => false)
  if (!valid) return

  uploadLoading.value = true
  try {
    const res = await uploadKbDocument({
      meta: {
        categoryId: uploadFormData.categoryId,
        title: uploadFormData.title,
        remark: uploadFormData.remark
      },
      file: uploadFormData.file
    })
    ElMessage.success('上传成功')
    uploadedDocumentId.value = res.data?.id || null
    imageUnderstandText.value = ''
    patchCurrentImageTask(null)
    await loadImageTaskList(uploadedDocumentId.value)
    await loadDocumentList()
  } finally {
    uploadLoading.value = false
  }
}

const handleSubmitUploadAndIndex = async () => {
  const valid = await uploadFormRef.value?.validate().catch(() => false)
  if (!valid) return

  uploadAndIndexLoading.value = true
  try {
    await uploadAndIndexKbDocument({
      meta: {
        categoryId: uploadFormData.categoryId,
        title: uploadFormData.title,
        remark: uploadFormData.remark
      },
      file: uploadFormData.file
    })
    ElMessage.success('上传并一键解析成功')
    uploadDialogVisible.value = false
    await loadDocumentList()
  } finally {
    uploadAndIndexLoading.value = false
  }
}

const handlePreviewImageUnderstand = async () => {
  if (!uploadedDocumentId.value) {
    ElMessage.warning('请先确认上传生成文档ID')
    return
  }

  imagePreviewLoading.value = true
  try {
    const res = await submitKbImageUnderstandPreviewTask(uploadedDocumentId.value)
    const task = res.data || {}
    saveLastTaskId(task.taskId)
    selectedImageTaskId.value = task.taskId || ''
    await handleTaskUpdated(task, true)
    await loadImageTaskList(uploadedDocumentId.value)
    startImageTaskPolling(task.taskId)
    ElMessage.success('图片解析任务已提交，可离开页面稍后返回')
  } finally {
    imagePreviewLoading.value = false
  }
}

const handleImageUnderstandIndex = async () => {
  if (!uploadedDocumentId.value) {
    ElMessage.warning('请先确认上传生成文档ID')
    return
  }

  const confirmed = await ElMessageBox.confirm('确认执行“图片理解后一键向量化”吗？', '提示', {
    type: 'warning',
    confirmButtonText: '开始入库',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  imageIndexLoading.value = true
  try {
    const targetTaskId = selectedImageTaskId.value || currentImageTask.taskId
    if (!targetTaskId) {
      ElMessage.warning('请先选择或提交一个图片理解任务')
      return
    }
    const res = await submitKbImageUnderstandIndexTaskByTaskId(targetTaskId, {
      imageUnderstandText: imageUnderstandText.value
    })
    const task = res.data || {}
    saveLastTaskId(task.taskId)
    selectedImageTaskId.value = task.taskId || ''
    await handleTaskUpdated(task, true)
    await loadImageTaskList(uploadedDocumentId.value)
    startImageTaskPolling(task.taskId)
    ElMessage.success('图片文本向量化任务已提交，可离开页面稍后返回')
  } finally {
    imageIndexLoading.value = false
  }
}

const openUpdateStatusDialog = (row) => {
  statusFormData.id = row.id
  statusFormData.title = row.title || ''
  statusFormData.processStatus = row.processStatus || 'PENDING'
  statusFormData.failReason = row.failReason || ''
  statusDialogVisible.value = true
}

const handleSubmitStatus = async () => {
  const valid = await statusFormRef.value?.validate().catch(() => false)
  if (!valid) return

  statusLoading.value = true
  try {
    await updateKbDocumentStatus(statusFormData.id, {
      processStatus: statusFormData.processStatus,
      failReason: statusFormData.failReason
    })
    ElMessage.success('状态更新成功')
    statusDialogVisible.value = false
    await loadDocumentList()
  } finally {
    statusLoading.value = false
  }
}

const handleRetry = async (row) => {
  const confirmed = await ElMessageBox.confirm(`确认重试文档「${row.title}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '重试',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  await retryKbDocument(row.id)
  ElMessage.success('已重试')
  await loadDocumentList()
}

const handleParse = async (row) => {
  const confirmed = await ElMessageBox.confirm(`确认解析文档「${row.title}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '解析',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  await parseKbDocument(row.id)
  ElMessage.success('解析任务已触发')
  await loadDocumentList()
}

const handleIndex = async (row) => {
  const confirmed = await ElMessageBox.confirm(`确认向量化入库文档「${row.title}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '开始入库',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  await indexKbDocument(row.id)
  ElMessage.success('向量化任务已触发')
  await loadDocumentList()
}

onMounted(async () => {
  await loadDocumentList()
  if (lastImageTaskId.value) {
    try {
      await queryTaskAndSync(lastImageTaskId.value, true)
      if (imageTaskRunning.value) {
        startImageTaskPolling(lastImageTaskId.value)
      }
    } catch (error) {
      saveLastTaskId('')
    }
  }
})

onBeforeUnmount(() => {
  stopImageTaskPolling()
})
</script>

<style scoped>
.kb-document-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}

.toolbar-card {
  flex-shrink: 0;
  min-height: 76px;
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.25);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 10px 24px rgba(123, 150, 219, 0.16);
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-left { min-width: 0; }
.toolbar-title {
  margin: 0;
  color: #243456;
  font-size: 18px;
  font-weight: 700;
}
.toolbar-desc {
  margin: 8px 0 0;
  color: rgba(84, 105, 152, 0.76);
  font-size: 13px;
}

.toolbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.24);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 24px rgba(123, 150, 219, 0.16);
  padding: 14px;
  overflow: hidden;
}

.table-wrap {
  flex: 1;
  min-height: 0;
}

.pagination-wrap {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 2px;
}

.table-card :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(126, 159, 241, 0.1);
  --el-table-border-color: rgba(133, 160, 230, 0.2);
  --el-table-row-hover-bg-color: rgba(126, 159, 241, 0.12);
  --el-table-text-color: #31446d;
  --el-table-header-text-color: #3d5384;
}

.full-width { width: 100%; }

.image-understand-panel {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(133, 160, 230, 0.38);
}

.image-understand-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.image-understand-tip {
  color: rgba(84, 105, 152, 0.72);
  font-size: 12px;
}

.image-task-picker {
  margin-bottom: 10px;
}

.image-task-status {
  margin-bottom: 10px;
  padding: 8px 10px;
  border: 1px solid rgba(133, 160, 230, 0.22);
  border-radius: 8px;
  background: rgba(246, 250, 255, 0.9);
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: #405688;
  font-size: 12px;
}
</style>
