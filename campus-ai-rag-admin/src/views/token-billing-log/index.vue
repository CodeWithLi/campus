<template>
  <div class="token-log-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">Token 消耗日志</h2>
        <p class="toolbar-desc">对齐后端 `/admin/token-billing-logs`，支持筛选与分页查询</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="用户ID">
          <el-input v-model="queryForm.userId" placeholder="例如 10001" clearable />
        </el-form-item>
        <el-form-item label="场景">
          <el-select v-model="queryForm.scene" placeholder="全部" clearable>
            <el-option label="CHAT" value="CHAT" />
            <el-option label="RAG" value="RAG" />
            <el-option label="RESUME" value="RESUME" />
            <el-option label="POST" value="POST" />
            <el-option label="INTERNAL" value="INTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型编码">
          <el-select v-model="queryForm.modelCode" filterable placeholder="全部" clearable>
            <el-option
              v-for="item in modelOptions"
              :key="item.modelCode"
              :label="`${item.modelName || item.modelCode} (${item.modelCode})`"
              :value="item.modelCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable>
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
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
        <el-table :data="logList" height="100%">
          <el-table-column prop="id" label="ID" min-width="70" />
          <el-table-column prop="userId" label="用户ID" min-width="90" />
          <el-table-column prop="scene" label="场景" min-width="100" />
          <el-table-column prop="modelCode" label="模型编码" min-width="140" />
          <el-table-column prop="invokeType" label="调用方式" min-width="90" />
          <el-table-column prop="billingType" label="计费类型" min-width="120" />
          <el-table-column prop="promptTokens" label="输入Token" min-width="100" />
          <el-table-column prop="completionTokens" label="输出Token" min-width="100" />
          <el-table-column prop="totalTokens" label="总Token" min-width="100" />
          <el-table-column prop="consumedPoints" label="扣减算力点" min-width="110" />
          <el-table-column prop="elapsedMs" label="响应耗时(ms)" min-width="120" />
          <el-table-column label="状态" min-width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="失败原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" min-width="180" />
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getModelConfigList, getTokenBillingLogPage } from '@/api/model'

const tableLoading = ref(false)
const logList = ref([])
const modelOptions = ref([])
const pageState = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const queryForm = reactive({
  userId: '',
  scene: '',
  modelCode: '',
  status: null,
  timeRange: []
})

const loadModelOptions = async () => {
  const res = await getModelConfigList()
  const list = res.data || []
  modelOptions.value = list
    .filter((item) => item?.enabled === 1)
    .sort((a, b) => (a?.sortNo ?? 0) - (b?.sortNo ?? 0))
}

const buildQueryParams = () => {
  const [startTime, endTime] = queryForm.timeRange || []
  return {
    pageNum: pageState.pageNum,
    pageSize: pageState.pageSize,
    userId: queryForm.userId ? Number(queryForm.userId) : null,
    scene: queryForm.scene || null,
    modelCode: queryForm.modelCode || null,
    status: queryForm.status,
    startTime: startTime || null,
    endTime: endTime || null
  }
}

const loadLogPage = async () => {
  tableLoading.value = true
  try {
    const res = await getTokenBillingLogPage(buildQueryParams())
    const pageData = res.data || {}
    logList.value = pageData.list || []
    pageState.total = pageData.total || 0
  } finally {
    tableLoading.value = false
  }
}

const handleSearch = async () => {
  pageState.pageNum = 1
  await loadLogPage()
}

const handleReset = async () => {
  queryForm.userId = ''
  queryForm.scene = ''
  queryForm.modelCode = ''
  queryForm.status = null
  queryForm.timeRange = []
  pageState.pageNum = 1
  pageState.pageSize = 20
  await loadLogPage()
}

const handlePageNumChange = async (value) => {
  pageState.pageNum = value
  await loadLogPage()
}

const handlePageSizeChange = async (value) => {
  pageState.pageSize = value
  pageState.pageNum = 1
  await loadLogPage()
}

onMounted(async () => {
  await loadModelOptions()
  await loadLogPage()
})
</script>

<style scoped>
.token-log-page {
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
</style>

