<template>
  <div class="dashboard-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">AI 运营看板</h2>
        <p class="toolbar-desc">关注调用健康、成本趋势与模型流量分配</p>
      </div>
      <div class="toolbar-right">
        <el-radio-group v-model="queryForm.granularity" size="small">
          <el-radio-button label="day">按天</el-radio-button>
          <el-radio-button label="hour">按小时</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="queryForm.timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="loading" @click="loadAllCharts">刷新</el-button>
      </div>
    </div>

    <div class="chart-card" v-loading="loading">
      <div class="card-title">AI 调用趋势（调用量 / 成功率 / 平均耗时）</div>
      <div ref="trendChartRef" class="chart-box" />
    </div>

    <div class="chart-card" v-loading="loading">
      <div class="card-title">Token 与积分消耗（双轴）</div>
      <div ref="consumptionChartRef" class="chart-box" />
    </div>

    <div class="chart-card" v-loading="loading">
      <div class="card-title">模型使用占比</div>
      <div ref="shareChartRef" class="chart-box" />
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { getAiCallTrend, getModelUsageShare, getTokenConsumptionTrend } from '@/api/model'

const loading = ref(false)
const trendChartRef = ref(null)
const consumptionChartRef = ref(null)
const shareChartRef = ref(null)

let trendChart = null
let consumptionChart = null
let shareChart = null

const queryForm = reactive({
  granularity: 'day',
  timeRange: []
})

const buildParams = () => {
  const [startTime, endTime] = queryForm.timeRange || []
  return {
    granularity: queryForm.granularity || 'day',
    startTime: startTime || null,
    endTime: endTime || null
  }
}

const initCharts = () => {
  if (!trendChart && trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
  }
  if (!consumptionChart && consumptionChartRef.value) {
    consumptionChart = echarts.init(consumptionChartRef.value)
  }
  if (!shareChart && shareChartRef.value) {
    shareChart = echarts.init(shareChartRef.value)
  }
}

const renderTrendChart = (rows = []) => {
  const xAxis = rows.map((item) => item.bucket)
  trendChart?.setOption({
    color: ['#5b8ff9', '#5ad8a6', '#ff9d4d'],
    tooltip: { trigger: 'axis' },
    legend: { top: 8 },
    grid: { top: 52, left: 48, right: 118, bottom: 36 },
    xAxis: { type: 'category', data: xAxis },
    yAxis: [
      {
        type: 'value',
        name: '调用次数',
        position: 'left'
      },
      {
        type: 'value',
        name: '成功率(%)',
        max: 100,
        position: 'right',
        offset: 0,
        axisLabel: {
          formatter: '{value}%'
        }
      },
      {
        type: 'value',
        name: '平均耗时(ms)',
        position: 'right',
        offset: 62,
        splitLine: { show: false },
        axisLabel: {
          formatter: (value) => Number(value || 0).toLocaleString()
        }
      }
    ],
    series: [
      { name: '调用次数', type: 'line', smooth: true, yAxisIndex: 0, data: rows.map((i) => i.callCount || 0) },
      { name: '成功率', type: 'line', smooth: true, yAxisIndex: 1, data: rows.map((i) => i.successRate || 0) },
      { name: '平均耗时', type: 'line', smooth: true, yAxisIndex: 2, data: rows.map((i) => i.avgElapsedMs || 0) }
    ]
  })
}

const renderConsumptionChart = (rows = []) => {
  const xAxis = rows.map((item) => item.bucket)
  consumptionChart?.setOption({
    color: ['#4c8dff', '#6dc8ec', '#8dd6a5', '#f6bd16'],
    tooltip: { trigger: 'axis' },
    legend: { top: 8 },
    grid: { top: 52, left: 36, right: 36, bottom: 36 },
    xAxis: { type: 'category', data: xAxis },
    yAxis: [
      { type: 'value', name: 'Token' },
      { type: 'value', name: '积分' }
    ],
    series: [
      { name: '输入 Tokens', type: 'bar', stack: 'token', yAxisIndex: 0, data: rows.map((i) => i.promptTokens || 0) },
      { name: '输出 Tokens', type: 'bar', stack: 'token', yAxisIndex: 0, data: rows.map((i) => i.completionTokens || 0) },
      { name: '总 Tokens', type: 'line', smooth: true, yAxisIndex: 0, data: rows.map((i) => i.totalTokens || 0) },
      { name: '扣减积分', type: 'line', smooth: true, yAxisIndex: 1, data: rows.map((i) => i.consumedPoints || 0) }
    ]
  })
}

const renderShareChart = (rows = []) => {
  const data = rows.map((item) => ({
    name: item.modelCode || 'unknown',
    value: item.callCount || 0
  }))
  shareChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { top: 8 },
    series: [
      {
        name: '模型占比',
        type: 'pie',
        radius: ['46%', '72%'],
        center: ['50%', '58%'],
        label: { formatter: '{b}: {d}%' },
        data
      }
    ]
  })
}

const loadAllCharts = async () => {
  loading.value = true
  try {
    const params = buildParams()
    const [trendRes, consumptionRes, shareRes] = await Promise.all([
      getAiCallTrend(params),
      getTokenConsumptionTrend(params),
      getModelUsageShare({ startTime: params.startTime, endTime: params.endTime })
    ])
    renderTrendChart(trendRes.data || [])
    renderConsumptionChart(consumptionRes.data || [])
    renderShareChart(shareRes.data || [])
  } finally {
    loading.value = false
  }
}

const handleReset = async () => {
  queryForm.granularity = 'day'
  queryForm.timeRange = []
  await loadAllCharts()
}

const onResize = () => {
  trendChart?.resize()
  consumptionChart?.resize()
  shareChart?.resize()
}

onMounted(async () => {
  await nextTick()
  initCharts()
  window.addEventListener('resize', onResize)
  await loadAllCharts()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  consumptionChart?.dispose()
  shareChart?.dispose()
  trendChart = null
  consumptionChart = null
  shareChart = null
})
</script>

<style scoped>
.dashboard-page {
  height: 100%;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.toolbar-card,
.chart-card {
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.24);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 24px rgba(123, 150, 219, 0.16);
}
.toolbar-card {
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
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
.toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.chart-card {
  padding: 14px;
}
.card-title {
  margin-bottom: 10px;
  color: #2f4471;
  font-size: 15px;
  font-weight: 700;
}
.chart-box {
  height: 320px;
}
</style>
