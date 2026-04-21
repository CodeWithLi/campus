<template>
  <div class="post-report-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">帖子举报管理</h2>
        <p class="toolbar-desc">处理用户举报，判定违规或无效，并可联动删除违规帖子</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部">
            <el-option label="待处理" value="PENDING" />
            <el-option label="有效举报" value="RESOLVED_VALID" />
            <el-option label="无效举报" value="RESOLVED_INVALID" />
          </el-select>
        </el-form-item>
        <el-form-item label="帖子ID">
          <el-input v-model="queryForm.postId" clearable placeholder="例如 123" />
        </el-form-item>
        <el-form-item label="举报人ID">
          <el-input v-model="queryForm.reporterUserId" clearable placeholder="例如 10001" />
        </el-form-item>
        <el-form-item label="举报时间">
          <el-date-picker
            v-model="queryForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-wrap" v-loading="tableLoading">
        <el-table :data="reportList" height="100%">
          <el-table-column prop="id" label="举报ID" min-width="90" />
          <el-table-column prop="postId" label="帖子ID" min-width="90" />
          <el-table-column prop="postTitle" label="帖子标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="reporterUserId" label="举报人ID" min-width="90" />
          <el-table-column prop="reporterNickname" label="举报人昵称" min-width="120" />
          <el-table-column prop="reason" label="举报理由" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="handledBy" label="处理人" min-width="120" />
          <el-table-column prop="handledAt" label="处理时间" min-width="170" />
          <el-table-column prop="createdAt" label="举报时间" min-width="170" />
          <el-table-column label="操作" fixed="right" min-width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'PENDING'" link type="success" @click="openHandleDialog(row)">处理</el-button>
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

    <el-dialog v-model="detailVisible" title="举报详情" width="780px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="举报ID">{{ detailData.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="帖子ID">{{ detailData.postId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="帖子标题" :span="2">{{ detailData.postTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="举报人">{{ detailData.reporterNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="举报人ID">{{ detailData.reporterUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="举报理由" :span="2">{{ detailData.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detailData.status) }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detailData.handledBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2">{{ detailData.handleRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ detailData.handledAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="举报时间">{{ detailData.createdAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="handleDialogVisible" title="处理举报" width="620px" destroy-on-close>
      <el-form ref="handleFormRef" :model="handleForm" label-width="120px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.status">
            <el-radio label="RESOLVED_VALID">有效举报</el-radio>
            <el-radio label="RESOLVED_INVALID">无效举报</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="联动操作" v-if="handleForm.status === 'RESOLVED_VALID'">
          <el-checkbox v-model="handleForm.deletePost">同时删除帖子</el-checkbox>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="handleForm.handleRemark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="建议填写处理说明，便于后续追踪"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleSubmitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPostReportDetail, getPostReportPage, handlePostReport } from '@/api/post-report'

const tableLoading = ref(false)
const reportList = ref([])
const pageState = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const queryForm = reactive({ status: '', postId: '', reporterUserId: '', timeRange: [] })

const detailVisible = ref(false)
const detailData = reactive({})

const handleDialogVisible = ref(false)
const handleSubmitting = ref(false)
const currentHandleId = ref(null)
const handleForm = reactive({ status: 'RESOLVED_VALID', deletePost: true, handleRemark: '' })

const buildQuery = () => {
  const [startTime, endTime] = queryForm.timeRange || []
  return {
    pageNum: pageState.pageNum,
    pageSize: pageState.pageSize,
    status: queryForm.status || null,
    postId: queryForm.postId ? Number(queryForm.postId) : null,
    reporterUserId: queryForm.reporterUserId ? Number(queryForm.reporterUserId) : null,
    startTime: startTime || null,
    endTime: endTime || null
  }
}

const loadReportPage = async () => {
  tableLoading.value = true
  try {
    const res = await getPostReportPage(buildQuery())
    const pageData = res.data || {}
    reportList.value = pageData.list || []
    pageState.total = pageData.total || 0
  } finally {
    tableLoading.value = false
  }
}

const handleSearch = async () => {
  pageState.pageNum = 1
  await loadReportPage()
}

const handleReset = async () => {
  queryForm.status = ''
  queryForm.postId = ''
  queryForm.reporterUserId = ''
  queryForm.timeRange = []
  pageState.pageNum = 1
  pageState.pageSize = 20
  await loadReportPage()
}

const handlePageNumChange = async (v) => {
  pageState.pageNum = v
  await loadReportPage()
}

const handlePageSizeChange = async (v) => {
  pageState.pageSize = v
  pageState.pageNum = 1
  await loadReportPage()
}

const openDetail = async (row) => {
  const res = await getPostReportDetail(row.id)
  Object.assign(detailData, res.data || {})
  detailVisible.value = true
}

const openHandleDialog = (row) => {
  currentHandleId.value = row.id
  handleForm.status = 'RESOLVED_VALID'
  handleForm.deletePost = true
  handleForm.handleRemark = ''
  handleDialogVisible.value = true
}

const submitHandle = async () => {
  if (!currentHandleId.value) return
  handleSubmitting.value = true
  try {
    await handlePostReport(currentHandleId.value, {
      status: handleForm.status,
      deletePost: handleForm.status === 'RESOLVED_VALID' ? Boolean(handleForm.deletePost) : false,
      handleRemark: handleForm.handleRemark || null
    })
    ElMessage.success('处理成功')
    handleDialogVisible.value = false
    await loadReportPage()
  } finally {
    handleSubmitting.value = false
  }
}

const statusLabel = (status) => {
  if (status === 'PENDING') return '待处理'
  if (status === 'RESOLVED_VALID') return '有效举报'
  if (status === 'RESOLVED_INVALID') return '无效举报'
  return status || '-'
}

const statusTagType = (status) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'RESOLVED_VALID') return 'danger'
  if (status === 'RESOLVED_INVALID') return 'success'
  return 'info'
}

loadReportPage()
</script>

<style scoped>
.post-report-page { height: 100%; display: flex; flex-direction: column; gap: 14px; overflow: hidden; }
.toolbar-card { flex-shrink: 0; min-height: 76px; border-radius: 16px; border: 1px solid rgba(133, 160, 230, 0.25); background: rgba(255, 255, 255, 0.86); box-shadow: 0 10px 24px rgba(123, 150, 219, 0.16); padding: 14px 18px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.toolbar-title { margin: 0; color: #243456; font-size: 18px; font-weight: 700; }
.toolbar-desc { margin: 8px 0 0; color: rgba(84, 105, 152, 0.76); font-size: 13px; }
.toolbar-actions { display: inline-flex; gap: 10px; }
.filter-card { flex-shrink: 0; border-radius: 16px; border: 1px solid rgba(133, 160, 230, 0.24); background: rgba(255, 255, 255, 0.9); box-shadow: 0 12px 24px rgba(123, 150, 219, 0.16); padding: 14px; }
.filter-form { display: flex; flex-wrap: wrap; }
.table-card { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 10px; border-radius: 16px; border: 1px solid rgba(133, 160, 230, 0.24); background: rgba(255, 255, 255, 0.9); box-shadow: 0 12px 24px rgba(123, 150, 219, 0.16); padding: 14px; overflow: hidden; }
.table-wrap { flex: 1; min-height: 0; }
.pagination-wrap { flex-shrink: 0; display: flex; justify-content: flex-end; padding-top: 2px; }
.table-card :deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: rgba(126, 159, 241, 0.1); --el-table-border-color: rgba(133, 160, 230, 0.2); --el-table-row-hover-bg-color: rgba(126, 159, 241, 0.12); --el-table-text-color: #31446d; --el-table-header-text-color: #3d5384; }
</style>
