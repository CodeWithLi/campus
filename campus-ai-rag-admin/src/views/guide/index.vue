<template>
  <div class="guide-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">办事指南管理</h2>
        <p class="toolbar-desc">支持新增、编辑、上下架与步骤配置</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="openCreateDialog">新增指南</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" clearable placeholder="标题或简介" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" clearable filterable placeholder="全部分类" style="width: 180px">
            <el-option
              v-for="item in categoryOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="上架" value="PUBLISHED" />
            <el-option label="下架" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-wrap" v-loading="tableLoading">
        <el-table :data="guideList" height="100%">
          <el-table-column prop="id" label="ID" min-width="80" />
          <el-table-column prop="title" label="指南标题" min-width="180" />
          <el-table-column prop="category" label="分类" min-width="110" />
          <el-table-column prop="tag" label="标签" min-width="90" />
          <el-table-column prop="summary" label="简介" min-width="220" show-overflow-tooltip />
          <el-table-column prop="sortNo" label="排序" min-width="90" />
          <el-table-column label="状态" min-width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
          <el-table-column label="操作" min-width="270" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="handlePublish(row)">上架</el-button>
              <el-button v-else link type="warning" @click="handleOffline(row)">下架</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增指南' : '编辑指南'" width="940px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model="formData.title" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select
                v-model="formData.category"
                filterable
                allow-create
                default-first-option
                clearable
                :reserve-keyword="false"
                placeholder="请选择或输入新分类"
                class="full-width"
              >
                <el-option
                  v-for="item in categoryOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标签">
              <el-input v-model="formData.tag" placeholder="如：高频" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="formData.sortNo" :min="0" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="简介">
          <el-input v-model="formData.summary" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-upload-wrap">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :http-request="handleCoverUpload"
              :before-upload="beforeCoverUpload"
            >
              <el-button type="primary" plain :loading="coverUploading">上传封面图</el-button>
            </el-upload>
            <div v-if="formData.coverUrl" class="cover-preview">
              <el-image :src="resolveCoverUrl(formData.coverUrl)" fit="cover" class="cover-image" />
              <div class="cover-url">{{ formData.coverUrl }}</div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="关联RAG">
          <el-select
            v-model="formData.ragCategoryIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            class="full-width"
            placeholder="选择知识库分类（用于RAG问答）"
          >
            <el-option
              v-for="c in kbCategoryOptions"
              :key="c.id"
              :label="c.categoryName"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <div class="step-head">
          <span>步骤配置</span>
          <el-button size="small" @click="addStep">新增步骤</el-button>
        </div>
        <div class="step-list">
          <div v-for="(step, index) in formData.steps" :key="index" class="step-card">
            <div class="step-top">
              <span>步骤 {{ index + 1 }}</span>
              <el-button link type="danger" @click="removeStep(index)">删除</el-button>
            </div>
            <el-row :gutter="12">
              <el-col :span="6">
                <el-input-number v-model="step.stepNo" :min="1" class="full-width" />
              </el-col>
              <el-col :span="18">
                <el-input v-model="step.title" placeholder="步骤标题" />
              </el-col>
            </el-row>
            <el-input v-model="step.content" type="textarea" :rows="2" placeholder="步骤说明" class="step-mt" />
            <el-row :gutter="12" class="step-mt">
              <el-col :span="8">
                <el-select v-model="step.actionType" class="full-width" @change="onStepActionTypeChange(step)">
                  <el-option label="无动作" value="NONE" />
                  <el-option label="跳转链接" value="LINK" />
                  <el-option label="创建提醒" value="REMINDER" />
                  <el-option label="问 AI" value="ASK_AI" />
                </el-select>
              </el-col>
              <el-col :span="16">
                <el-input
                  v-if="step.actionType === 'LINK'"
                  v-model="step.actionForm.url"
                  placeholder="请输入跳转链接，例如 https://..."
                />
                <el-input
                  v-else-if="step.actionType === 'ASK_AI'"
                  v-model="step.actionForm.prompt"
                  placeholder="请输入要提给 AI 的问题"
                />
                <el-row v-else-if="step.actionType === 'REMINDER'" :gutter="8">
                  <el-col :span="10">
                    <el-input v-model="step.actionForm.title" placeholder="提醒标题" />
                  </el-col>
                  <el-col :span="7">
                    <el-input-number v-model="step.actionForm.offsetHours" :min="1" :max="720" class="full-width" />
                  </el-col>
                  <el-col :span="7">
                    <el-select v-model="step.actionForm.type" class="full-width">
                      <el-option label="活动" value="activity" />
                      <el-option label="DDL" value="ddl" />
                      <el-option label="考试" value="exam" />
                    </el-select>
                  </el-col>
                </el-row>
                <el-input
                  v-else
                  value=""
                  disabled
                  placeholder="当前动作无需额外参数"
                />
              </el-col>
            </el-row>
            <el-row :gutter="12" class="step-mt">
              <el-col :span="16">
                <div class="materials-editor">
                  <div class="materials-label">材料清单</div>
                  <div class="materials-template-row">
                    <el-select v-model="step.selectedTemplateId" filterable clearable class="template-select" placeholder="套用材料模板">
                      <el-option
                        v-for="t in materialTemplateOptions"
                        :key="t.id"
                        :label="t.templateName"
                        :value="t.id"
                      />
                    </el-select>
                    <el-button @click="applyMaterialTemplate(step)" :disabled="!step.selectedTemplateId">套用</el-button>
                  </div>
                  <div class="materials-tags">
                    <el-tag
                      v-for="(material, materialIndex) in step.materialsList"
                      :key="`${material}-${materialIndex}`"
                      type="info"
                      effect="light"
                      closable
                      @close="removeMaterial(step, materialIndex)"
                    >
                      {{ material }}
                    </el-tag>
                    <span v-if="!step.materialsList.length" class="materials-empty">暂无材料，先添加一项吧</span>
                  </div>
                  <div class="materials-input-row">
                    <el-input
                      v-model="step.materialInput"
                      placeholder="例如：学生证、请假证明、导师签字表"
                      @keyup.enter="addMaterial(step)"
                    />
                    <el-button type="primary" plain @click="addMaterial(step)">添加</el-button>
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <el-select v-model="step.status" class="full-width">
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createGuide,
  deleteGuide,
  getGuideDetail,
  getGuidePage,
  offlineGuide,
  publishGuide,
  uploadGuideCover,
  updateGuide
} from '@/api/guide'
import { getKbCategoryList } from '@/api/kb'
import { getMaterialTemplateList } from '@/api/material-template'

const tableLoading = ref(false)
const submitLoading = ref(false)
const coverUploading = ref(false)
const guideList = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentGuideId = ref(null)
const formRef = ref()
const categoryOptions = ref([])
const kbCategoryOptions = ref([])
const materialTemplateOptions = ref([])

const pageState = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const queryForm = reactive({ keyword: '', category: '', status: '' })

const createStep = () => ({
  stepNo: 1,
  title: '',
  content: '',
  actionType: 'NONE',
  actionPayload: '',
  actionForm: buildDefaultActionForm('NONE'),
  requiredMaterials: '',
  materialsList: [],
  materialInput: '',
  selectedTemplateId: 0,
  status: 1
})

const createFormData = () => ({
  title: '',
  category: '',
  tag: '',
  summary: '',
  coverUrl: '',
  ragCategoryIds: [],
  sortNo: 100,
  steps: [createStep()]
})

const formData = reactive(createFormData())

const formRules = {
  title: [{ required: true, message: '请输入指南标题', trigger: 'blur' }]
}

const resetFormData = () => {
  Object.assign(formData, createFormData())
}

const buildQueryParams = () => ({
  pageNum: pageState.pageNum,
  pageSize: pageState.pageSize,
  keyword: queryForm.keyword || null,
  category: queryForm.category || null,
  status: queryForm.status || null
})

const loadGuidePage = async () => {
  tableLoading.value = true
  try {
    const res = await getGuidePage(buildQueryParams())
    const pageData = res.data || {}
    guideList.value = pageData.list || []
    pageState.total = pageData.total || 0
    syncCategoryOptions(guideList.value)
  } finally {
    tableLoading.value = false
  }
}

const syncCategoryOptions = (rows = []) => {
  const categorySet = new Set(categoryOptions.value)
  ;(rows || []).forEach((item) => {
    const category = String(item?.category || '').trim()
    if (category) categorySet.add(category)
  })
  categoryOptions.value = Array.from(categorySet)
}

const ensureCategoryOption = (category) => {
  const value = String(category || '').trim()
  if (!value) return
  if (categoryOptions.value.includes(value)) return
  categoryOptions.value = [...categoryOptions.value, value]
}

const statusText = (status) => {
  if (status === 'PUBLISHED') return '已上架'
  if (status === 'OFFLINE') return '已下架'
  return '草稿'
}

const statusType = (status) => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'OFFLINE') return 'warning'
  return 'info'
}

const handleSearch = async () => {
  pageState.pageNum = 1
  await loadGuidePage()
}

const handleReset = async () => {
  queryForm.keyword = ''
  queryForm.category = ''
  queryForm.status = ''
  pageState.pageNum = 1
  pageState.pageSize = 20
  await loadGuidePage()
}

const handlePageNumChange = async (value) => {
  pageState.pageNum = value
  await loadGuidePage()
}

const handlePageSizeChange = async (value) => {
  pageState.pageSize = value
  pageState.pageNum = 1
  await loadGuidePage()
}

const addStep = () => {
  formData.steps.push({
    ...createStep(),
    stepNo: formData.steps.length + 1
  })
}

const removeStep = (index) => {
  if (formData.steps.length <= 1) {
    ElMessage.warning('至少保留一个步骤')
    return
  }
  formData.steps.splice(index, 1)
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  currentGuideId.value = null
  resetFormData()
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  dialogMode.value = 'edit'
  currentGuideId.value = row.id
  resetFormData()
  dialogVisible.value = true
  const res = await getGuideDetail(row.id)
  const detail = res.data || {}
  let ragCategoryIds = []
  try {
    const parsed = JSON.parse(detail.ragCategoryIdsJson || '[]')
    ragCategoryIds = Array.isArray(parsed) ? parsed.map((x) => Number(x)).filter((x) => Number.isFinite(x) && x > 0) : []
  } catch (e) {
    ragCategoryIds = []
  }
  Object.assign(formData, {
    ...createFormData(),
    ...detail,
    ragCategoryIds,
    steps: Array.isArray(detail.steps) && detail.steps.length
      ? detail.steps.map((step) => normalizeStepForForm(step))
      : [createStep()]
  })
  ensureCategoryOption(formData.category)
}

const buildPayload = () => ({
  title: formData.title,
  category: formData.category,
  tag: formData.tag,
  summary: formData.summary,
  coverUrl: formData.coverUrl,
  ragCategoryIdsJson: formData.ragCategoryIds?.length ? JSON.stringify(formData.ragCategoryIds) : '',
  sortNo: formData.sortNo,
  steps: (formData.steps || []).map((step, index) => ({
    stepNo: Number(step.stepNo || index + 1),
    title: step.title,
    content: step.content,
    actionType: step.actionType,
    actionPayload: stringifyActionPayload(step),
    requiredMaterials: step.materialsList?.length ? JSON.stringify(step.materialsList) : '',
    status: Number(step.status === 0 ? 0 : 1)
  }))
})

const loadKbCategories = async () => {
  try {
    const res = await getKbCategoryList()
    const list = Array.isArray(res?.data) ? res.data : []
    kbCategoryOptions.value = list
  } catch (e) {
    kbCategoryOptions.value = []
  }
}

const loadMaterialTemplates = async () => {
  try {
    const res = await getMaterialTemplateList()
    materialTemplateOptions.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) {
    materialTemplateOptions.value = []
  }
}

const applyMaterialTemplate = (step) => {
  const id = Number(step?.selectedTemplateId || 0)
  if (!id) return
  const tpl = (materialTemplateOptions.value || []).find((t) => Number(t?.id) === id)
  if (!tpl) return
  let items = []
  try {
    const parsed = JSON.parse(tpl.itemsJson || '[]')
    items = Array.isArray(parsed) ? parsed.map((x) => String(x || '').trim()).filter(Boolean) : []
  } catch (e) {
    items = []
  }
  step.materialsList = items
  ElMessage.success('已套用材料模板')
}

const onStepActionTypeChange = (step) => {
  if (!step) return
  step.actionForm = buildDefaultActionForm(step.actionType)
}

const addMaterial = (step) => {
  if (!step) return
  const value = String(step.materialInput || '').trim()
  if (!value) return
  if (step.materialsList.includes(value)) {
    step.materialInput = ''
    return
  }
  step.materialsList = [...step.materialsList, value]
  step.materialInput = ''
}

const removeMaterial = (step, materialIndex) => {
  if (!step) return
  const next = [...(step.materialsList || [])]
  next.splice(materialIndex, 1)
  step.materialsList = next
}

function buildDefaultActionForm(actionType) {
  const type = String(actionType || 'NONE').toUpperCase()
  if (type === 'LINK') {
    return { url: '' }
  }
  if (type === 'REMINDER') {
    return { title: '', offsetHours: 24, type: 'activity' }
  }
  if (type === 'ASK_AI') {
    return { prompt: '' }
  }
  return {}
}

const parseActionPayload = (actionPayload) => {
  if (!actionPayload) return {}
  if (typeof actionPayload === 'object') return actionPayload
  try {
    const parsed = JSON.parse(actionPayload)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    return {}
  }
}

const normalizeStepForForm = (step = {}) => {
  const actionType = String(step.actionType || 'NONE').toUpperCase()
  const payloadObj = parseActionPayload(step.actionPayload)
  const actionForm = buildDefaultActionForm(actionType)
  const materialsList = parseMaterials(step.requiredMaterials)
  if (actionType === 'LINK') {
    actionForm.url = payloadObj.url || payloadObj.link || ''
  } else if (actionType === 'REMINDER') {
    actionForm.title = payloadObj.title || ''
    actionForm.offsetHours = Number(payloadObj.offsetHours || 24) || 24
    actionForm.type = payloadObj.type || 'activity'
  } else if (actionType === 'ASK_AI') {
    actionForm.prompt = payloadObj.prompt || ''
  }
  return {
    ...createStep(),
    ...step,
    actionType,
    actionPayload: typeof step.actionPayload === 'string' ? step.actionPayload : '',
    actionForm,
    materialsList,
    materialInput: ''
  }
}

const stringifyActionPayload = (step = {}) => {
  const actionType = String(step.actionType || 'NONE').toUpperCase()
  const actionForm = step.actionForm || {}
  if (actionType === 'LINK') {
    const payload = { url: String(actionForm.url || '').trim() }
    return payload.url ? JSON.stringify(payload) : ''
  }
  if (actionType === 'REMINDER') {
    const payload = {
      title: String(actionForm.title || '').trim(),
      offsetHours: Number(actionForm.offsetHours || 24),
      type: String(actionForm.type || 'activity')
    }
    return JSON.stringify(payload)
  }
  if (actionType === 'ASK_AI') {
    const payload = { prompt: String(actionForm.prompt || '').trim() }
    return payload.prompt ? JSON.stringify(payload) : ''
  }
  return ''
}

const parseMaterials = (requiredMaterials) => {
  if (!requiredMaterials) return []
  if (Array.isArray(requiredMaterials)) {
    return requiredMaterials.map((it) => String(it || '').trim()).filter(Boolean)
  }
  if (typeof requiredMaterials !== 'string') return []
  try {
    const parsed = JSON.parse(requiredMaterials)
    if (Array.isArray(parsed)) {
      return parsed.map((it) => String(it || '').trim()).filter(Boolean)
    }
  } catch (e) {
    // ignore
  }
  return []
}

const beforeCoverUpload = (file) => {
  const isImage = String(file?.type || '').startsWith('image/')
  if (!isImage) {
    ElMessage.warning('请上传图片格式文件')
    return false
  }
  const maxSizeMb = 5
  if ((file.size || 0) > maxSizeMb * 1024 * 1024) {
    ElMessage.warning(`图片大小不能超过 ${maxSizeMb}MB`)
    return false
  }
  return true
}

const handleCoverUpload = async (options) => {
  const file = options?.file
  if (!file) return
  coverUploading.value = true
  try {
    const res = await uploadGuideCover(file)
    formData.coverUrl = res?.data || ''
    ElMessage.success('封面图上传成功')
    options?.onSuccess?.(res)
  } catch (e) {
    options?.onError?.(e)
  } finally {
    coverUploading.value = false
  }
}

const resolveCoverUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  if (String(url).startsWith('/')) return url
  return `/${url}`
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!(formData.steps || []).length) {
    ElMessage.warning('请至少配置一个步骤')
    return
  }
  if (formData.steps.some((it) => !it.title || !it.title.trim())) {
    ElMessage.warning('步骤标题不能为空')
    return
  }
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createGuide(buildPayload())
      ElMessage.success('新增成功')
    } else {
      await updateGuide(currentGuideId.value, buildPayload())
      ElMessage.success('更新成功')
    }
    ensureCategoryOption(formData.category)
    dialogVisible.value = false
    await loadGuidePage()
  } finally {
    submitLoading.value = false
  }
}

const handlePublish = async (row) => {
  await publishGuide(row.id)
  ElMessage.success('已上架')
  await loadGuidePage()
}

const handleOffline = async (row) => {
  await offlineGuide(row.id)
  ElMessage.success('已下架')
  await loadGuidePage()
}

const handleDelete = async (row) => {
  const confirmed = await ElMessageBox.confirm(`确认删除指南「${row.title}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return
  await deleteGuide(row.id)
  ElMessage.success('删除成功')
  await loadGuidePage()
}

loadGuidePage()
loadKbCategories()
loadMaterialTemplates()
</script>

<style scoped>
.guide-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}
.toolbar-card,.filter-card,.table-card {
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.24);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 24px rgba(123, 150, 219, 0.16);
}
.toolbar-card {
  min-height: 76px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toolbar-title { margin: 0; color: #243456; font-size: 18px; font-weight: 700; }
.toolbar-desc { margin: 8px 0 0; color: rgba(84, 105, 152, 0.76); font-size: 13px; }
.toolbar-actions { display: inline-flex; gap: 10px; }
.filter-card { padding: 14px; }
.table-card { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 14px; }
.table-wrap { flex: 1; min-height: 0; }
.pagination-wrap { display: flex; justify-content: flex-end; padding-top: 8px; }
.table-card :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(126, 159, 241, 0.1);
}
.full-width { width: 100%; }
.cover-upload-wrap { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; width: 100%; }
.cover-preview { display: flex; align-items: center; gap: 10px; width: 100%; }
.cover-image { width: 120px; height: 68px; border-radius: 8px; border: 1px solid rgba(133, 160, 230, 0.24); background: #f8fbff; }
.cover-url { color: #60749f; font-size: 12px; word-break: break-all; }
.materials-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(133, 160, 230, 0.2);
  border-radius: 8px;
  background: rgba(245, 248, 255, 0.55);
}
.materials-label {
  font-size: 12px;
  color: #5b6f99;
  font-weight: 600;
}
.materials-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 32px;
  padding: 2px 0;
}
.materials-empty {
  font-size: 12px;
  color: #9aa8c3;
}
.materials-input-row { display: flex; gap: 8px; }
.materials-input-row :deep(.el-input__wrapper) {
  border-radius: 8px;
}
.materials-template-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.template-select {
  flex: 1;
}
.step-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; color: #2d426e; font-weight: 600; }
.step-list { max-height: 360px; overflow: auto; display: flex; flex-direction: column; gap: 10px; }
.step-card { border: 1px solid rgba(133, 160, 230, 0.24); border-radius: 10px; padding: 10px; }
.step-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; color: #3a507f; }
.step-mt { margin-top: 8px; }
</style>
