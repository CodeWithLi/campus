<template>
  <div class="post-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">帖子管理</h2>
        <p class="toolbar-desc">查看社区帖子内容、互动数据，支持筛选、详情与删除</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" clearable placeholder="标题或正文关键词" />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="queryForm.userId" clearable placeholder="例如 10001" />
        </el-form-item>
        <el-form-item label="发布方式">
          <el-select v-model="queryForm.isAnonymous" clearable placeholder="全部">
            <el-option label="实名" :value="0" />
            <el-option label="匿名" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
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
        <el-table :data="postList" height="100%">
          <el-table-column prop="id" label="ID" min-width="80" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="userId" label="用户ID" min-width="90" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column label="发布方式" min-width="100">
            <template #default="{ row }">
              <el-tag :type="row.isAnonymous === 1 ? 'warning' : 'success'">
                {{ row.isAnonymous === 1 ? '匿名' : '实名' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="文本合规" min-width="110">
            <template #default="{ row }">
              <el-tag :type="row.textComplianceSkipped === 1 ? 'info' : 'success'">
                {{ row.textComplianceSkipped === 1 ? '跳过' : '已校验' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="likeCount" label="点赞" min-width="80" />
          <el-table-column prop="commentCount" label="评论" min-width="80" />
          <el-table-column prop="createdAt" label="发布时间" min-width="170" />
          <el-table-column label="操作" fixed="right" min-width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="帖子详情" width="820px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="帖子ID">{{ detailData.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detailData.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detailData.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detailData.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailData.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布方式">{{ detailData.isAnonymous === 1 ? '匿名' : '实名' }}</el-descriptions-item>
        <el-descriptions-item label="点赞数">{{ detailData.likeCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="评论数">{{ detailData.commentCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ detailData.createdAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文本合规">{{ detailData.textComplianceSkipped === 1 ? '跳过' : '已校验' }}</el-descriptions-item>
        <el-descriptions-item label="正文" :span="2">
          <div class="content-block">{{ detailData.content || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="图片" :span="2">
          <div v-if="(detailData.imageUrls || []).length" class="image-list">
            <el-image
              v-for="(url, idx) in detailData.imageUrls"
              :key="`${url}-${idx}`"
              :src="url"
              fit="cover"
              class="post-image"
              :preview-src-list="detailData.imageUrls"
              :initial-index="idx"
              preview-teleported
            />
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deletePost, getPostDetail, getPostPage } from '@/api/post'

const tableLoading = ref(false)
const postList = ref([])
const pageState = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const queryForm = reactive({
  keyword: '',
  userId: '',
  isAnonymous: null,
  timeRange: []
})

const detailVisible = ref(false)
const detailData = reactive({
  imageUrls: []
})

const buildQueryParams = () => {
  const [startTime, endTime] = queryForm.timeRange || []
  return {
    pageNum: pageState.pageNum,
    pageSize: pageState.pageSize,
    keyword: queryForm.keyword || null,
    userId: queryForm.userId ? Number(queryForm.userId) : null,
    isAnonymous: queryForm.isAnonymous,
    startTime: startTime || null,
    endTime: endTime || null
  }
}

const loadPostPage = async () => {
  tableLoading.value = true
  try {
    const res = await getPostPage(buildQueryParams())
    const pageData = res.data || {}
    postList.value = pageData.list || []
    pageState.total = pageData.total || 0
  } finally {
    tableLoading.value = false
  }
}

const handleSearch = async () => {
  pageState.pageNum = 1
  await loadPostPage()
}

const handleReset = async () => {
  queryForm.keyword = ''
  queryForm.userId = ''
  queryForm.isAnonymous = null
  queryForm.timeRange = []
  pageState.pageNum = 1
  pageState.pageSize = 20
  await loadPostPage()
}

const handlePageNumChange = async (value) => {
  pageState.pageNum = value
  await loadPostPage()
}

const handlePageSizeChange = async (value) => {
  pageState.pageSize = value
  pageState.pageNum = 1
  await loadPostPage()
}

const openDetail = async (row) => {
  const res = await getPostDetail(row.id)
  Object.assign(detailData, { imageUrls: [], ...(res.data || {}) })
  detailVisible.value = true
}

const handleDelete = async (row) => {
  const confirmed = await ElMessageBox.confirm(
    `确认删除帖子「${row.title || row.id}」吗？删除后不可恢复。`,
    '提示',
    {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    }
  ).catch(() => false)
  if (!confirmed) return

  await deletePost(row.id)
  ElMessage.success('删除成功')
  await loadPostPage()
}

loadPostPage()
</script>

<style scoped>
.post-page {
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

.toolbar-left {
  min-width: 0;
}

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
  gap: 10px;
}

.filter-card {
  flex-shrink: 0;
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.24);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 24px rgba(123, 150, 219, 0.16);
  padding: 14px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
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

.content-block {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.post-image {
  width: 72px;
  height: 72px;
  border-radius: 8px;
}
</style>
