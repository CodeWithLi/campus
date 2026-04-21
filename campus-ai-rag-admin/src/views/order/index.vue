<template>
  <div class="order-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">订单管理</h2>
        <p class="toolbar-desc">查看充值订单状态、金额、交易号与支付时间，支持筛选与分页</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <div class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="订单号">
          <el-input v-model="queryForm.orderNo" clearable placeholder="支持模糊查询" />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="queryForm.userId" clearable placeholder="例如 10001" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部">
            <el-option label="CREATED（待支付）" value="CREATED" />
            <el-option label="PAID（已支付）" value="PAID" />
            <el-option label="CLOSED（已关闭）" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-input v-model="queryForm.channel" clearable placeholder="例如 ALIPAY_WAP" />
        </el-form-item>
        <el-form-item label="创建时间">
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
        <el-table :data="orderList" height="100%">
          <el-table-column prop="id" label="ID" min-width="80" />
          <el-table-column prop="orderNo" label="订单号" min-width="220" show-overflow-tooltip />
          <el-table-column prop="userId" label="用户ID" min-width="90" />
          <el-table-column prop="username" label="用户名" min-width="130" />
          <el-table-column prop="nickname" label="昵称" min-width="130" />
          <el-table-column prop="channel" label="渠道" min-width="120" />
          <el-table-column prop="amountYuan" label="金额(元)" min-width="100" />
          <el-table-column prop="points" label="算力点" min-width="100" />
          <el-table-column label="状态" min-width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ row.status || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="tradeNo" label="交易号" min-width="200" show-overflow-tooltip />
          <el-table-column prop="paidAt" label="支付时间" min-width="170" />
          <el-table-column prop="createdAt" label="创建时间" min-width="170" />
          <el-table-column label="操作" fixed="right" min-width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="720px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ detailData.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detailData.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detailData.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detailData.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailData.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ detailData.channel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额(元)">{{ detailData.amountYuan ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="算力点">{{ detailData.points ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailData.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="买家账号">{{ detailData.buyerLogonId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易号" :span="2">{{ detailData.tradeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单标题" :span="2">{{ detailData.subject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detailData.paidAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createdAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updatedAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { getRechargeOrderDetail, getRechargeOrderPage } from '@/api/order'

const tableLoading = ref(false)
const orderList = ref([])
const pageState = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const queryForm = reactive({
  orderNo: '',
  userId: '',
  status: '',
  channel: '',
  timeRange: []
})

const detailVisible = ref(false)
const detailData = reactive({})

const buildQueryParams = () => {
  const [startTime, endTime] = queryForm.timeRange || []
  return {
    pageNum: pageState.pageNum,
    pageSize: pageState.pageSize,
    orderNo: queryForm.orderNo || null,
    userId: queryForm.userId ? Number(queryForm.userId) : null,
    status: queryForm.status || null,
    channel: queryForm.channel || null,
    startTime: startTime || null,
    endTime: endTime || null
  }
}

const loadOrderPage = async () => {
  tableLoading.value = true
  try {
    const res = await getRechargeOrderPage(buildQueryParams())
    const pageData = res.data || {}
    orderList.value = pageData.list || []
    pageState.total = pageData.total || 0
  } finally {
    tableLoading.value = false
  }
}

const handleSearch = async () => {
  pageState.pageNum = 1
  await loadOrderPage()
}

const handleReset = async () => {
  queryForm.orderNo = ''
  queryForm.userId = ''
  queryForm.status = ''
  queryForm.channel = ''
  queryForm.timeRange = []
  pageState.pageNum = 1
  pageState.pageSize = 20
  await loadOrderPage()
}

const handlePageNumChange = async (value) => {
  pageState.pageNum = value
  await loadOrderPage()
}

const handlePageSizeChange = async (value) => {
  pageState.pageSize = value
  pageState.pageNum = 1
  await loadOrderPage()
}

const openDetail = async (row) => {
  const res = await getRechargeOrderDetail(row.id)
  Object.assign(detailData, res.data || {})
  detailVisible.value = true
}

const statusTagType = (status) => {
  if (status === 'PAID') return 'success'
  if (status === 'CREATED') return 'warning'
  if (status === 'CLOSED') return 'info'
  return ''
}

loadOrderPage()
</script>

<style scoped>
.order-page {
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
</style>
