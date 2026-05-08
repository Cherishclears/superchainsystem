<template>
  <div class="dashboard-page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">数据看板</h2>
        <span class="page-date">{{ todayLabel }}</span>
      </div>
    </div>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :span="6">
        <div class="kpi-card" style="--color:#1890ff; --bg:#e6f4ff">
          <div class="kpi-icon-wrap"><el-icon :size="22" color="#1890ff"><Money /></el-icon></div>
          <div class="kpi-info">
            <div class="kpi-value">¥ {{ stats.todaySaleAmount || 0 }}</div>
            <div class="kpi-label">今日销售额</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="--color:#52c41a; --bg:#f6ffed">
          <div class="kpi-icon-wrap"><el-icon :size="22" color="#52c41a"><ShoppingBag /></el-icon></div>
          <div class="kpi-info">
            <div class="kpi-value">{{ stats.todayOrderCount || 0 }}</div>
            <div class="kpi-label">今日订单数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="--color:#fa8c16; --bg:#fff7e6">
          <div class="kpi-icon-wrap"><el-icon :size="22" color="#fa8c16"><TrendCharts /></el-icon></div>
          <div class="kpi-info">
            <div class="kpi-value">¥ {{ stats.todayAvgAmount || 0 }}</div>
            <div class="kpi-label">今日客单价</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="--color:#eb2f96; --bg:#fff0f6">
          <div class="kpi-icon-wrap"><el-icon :size="22" color="#eb2f96"><User /></el-icon></div>
          <div class="kpi-info">
            <div class="kpi-value">{{ stats.todayMemberCount || 0 }}</div>
            <div class="kpi-label">今日新增会员</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 时间筛选 -->
    <div class="filter-bar">
      <span class="filter-label">时间范围</span>
      <el-radio-group v-model="timeRange" size="small" @change="handleTimeRangeChange">
        <el-radio-button value="7">近7天</el-radio-button>
        <el-radio-button value="30">近30天</el-radio-button>
        <el-radio-button value="custom">自定义</el-radio-button>
      </el-radio-group>
      <el-date-picker
        v-if="timeRange === 'custom'"
        v-model="customDateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        size="small"
        style="width: 240px; margin-left: 12px"
        @change="handleCustomDateChange"
      />
    </div>

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="card-title">销售趋势</span></template>
          <div ref="lineChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="card-title">商品分类销售占比</span></template>
          <div v-if="stats.categoryStats && stats.categoryStats.length > 0"
               ref="pieChartRef" style="height: 300px"></div>
          <el-empty v-else description="暂无数据" style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="chart-card rank-card">
          <template #header><span class="card-title">热销商品 TOP5</span></template>
          <div v-if="stats.topProducts && stats.topProducts.length">
            <div v-for="(item, index) in stats.topProducts" :key="item.productId" class="rank-item">
              <span class="rank-no" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <span class="rank-name">{{ item.productName }}</span>
              <span class="rank-amount">¥{{ item.totalAmount }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>

      <!-- AI 运营月报 -->
      <el-col :span="16">
        <div class="ai-report-card">
          <!-- 渐变头部 -->
          <div class="ai-report-header">
            <div class="ai-report-title">
              <el-icon :size="17"><MagicStick /></el-icon>
              <span>AI 运营月报</span>
              <span class="ai-month-badge">{{ lastMonthLabel }}</span>
            </div>
            <div class="ai-report-actions">
              <button class="ai-btn" :disabled="reportLoading" @click="generateReport">
                <el-icon v-if="reportLoading" class="btn-spin"><Loading /></el-icon>
                {{ reportContent ? '重新生成' : '生成报告' }}
              </button>
              <button v-if="reportContent" class="ai-btn" @click="exportReport">导出 Word</button>
            </div>
          </div>

          <!-- 内容区 -->
          <div class="ai-report-body">
            <!-- 空状态 -->
            <div v-if="!reportContent && !reportLoading" class="ai-empty">
              <el-icon :size="44" class="ai-empty-icon"><DataAnalysis /></el-icon>
              <p class="ai-empty-title">暂无报告</p>
              <p class="ai-empty-desc">点击「生成报告」，AI 将自动分析 <strong>{{ lastMonthLabel }}</strong> 的经营数据并生成运营建议</p>
            </div>

            <!-- 加载中（无内容） -->
            <div v-if="reportLoading && !reportContent" class="ai-loading">
              <div class="ai-dots"><span></span><span></span><span></span></div>
              <p>AI 正在分析数据，请稍候...</p>
            </div>

            <!-- 报告内容 -->
            <div v-if="reportContent" class="ai-content" v-html="renderedReport"></div>
            <span v-if="reportLoading && reportContent" class="typing-cursor"></span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'
import { useStoreStore } from '../../stores/store'
import { ElMessage } from 'element-plus'

const storeStore = useStoreStore()

const stats = ref({})
const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

const timeRange = ref('7')
const customDateRange = ref(null)
const startDate = ref(null)
const endDate = ref(null)

const reportContent = ref('')
const reportLoading = ref(false)

const todayLabel = computed(() => {
  const d = new Date()
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日  星期${days[d.getDay()]}`
})

const lastMonthLabel = computed(() => {
  const d = new Date()
  d.setMonth(d.getMonth() - 1)
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

// 逐行解析 markdown，生成带样式的 HTML
const applyInline = text =>
  text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

const renderedReport = computed(() => {
  if (!reportContent.value) return ''
  const lines = reportContent.value.replace(/\\n/g, '\n').split('\n')
  const parts = []
  let inList = false

  for (const raw of lines) {
    const line = raw.trim()
    if (!line) {
      if (inList) { parts.push('</ul>'); inList = false }
      continue
    }
    if (/^## /.test(line)) {
      if (inList) { parts.push('</ul>'); inList = false }
      parts.push(`<h2>${applyInline(line.slice(3))}</h2>`)
    } else if (/^### /.test(line)) {
      if (inList) { parts.push('</ul>'); inList = false }
      parts.push(`<h3>${applyInline(line.slice(4))}</h3>`)
    } else if (/^[-•*] /.test(line)) {
      if (!inList) { parts.push('<ul>'); inList = true }
      parts.push(`<li>${applyInline(line.slice(2))}</li>`)
    } else {
      if (inList) { parts.push('</ul>'); inList = false }
      parts.push(`<p>${applyInline(line)}</p>`)
    }
  }
  if (inList) parts.push('</ul>')
  return parts.join('')
})

const generateReport = async () => {
  reportContent.value = ''
  reportLoading.value = true
  try {
    const token = localStorage.getItem('token')
    const storeId = storeStore.currentStoreId
    const url = `http://localhost:8080/stats/report${storeId ? '?storeId=' + storeId : ''}`
    const response = await fetch(url, { headers: { Authorization: 'Bearer ' + token } })
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value, { stream: true })
      for (const line of chunk.split('\n')) {
        if (line.startsWith('data: ')) {
          const text = line.slice(6)
          if (text === '[DONE]') { reportLoading.value = false; return }
          reportContent.value += text
        }
      }
    }
  } catch {
    ElMessage.error('生成失败，请重试')
  } finally {
    reportLoading.value = false
  }
}

const exportReport = () => {
  const content = reportContent.value.replace(/\\n/g, '\n')
  const html = `<html xmlns:o='urn:schemas-microsoft-com:office:office'
    xmlns:w='urn:schemas-microsoft-com:office:word'
    xmlns='http://www.w3.org/TR/REC-html40'>
    <head><meta charset='utf-8'><title>运营月报</title></head>
    <body><h2>${lastMonthLabel.value} 运营月报</h2>
    <pre style="font-family:微软雅黑;font-size:14px;line-height:1.8">${content}</pre>
    </body></html>`
  const blob = new Blob(['\ufeff' + html], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${lastMonthLabel.value}运营月报.doc`
  a.click()
  URL.revokeObjectURL(url)
}

const handleTimeRangeChange = val => {
  if (val !== 'custom') {
    const days = parseInt(val)
    const now = new Date()
    const start = new Date(now)
    start.setDate(now.getDate() - days + 1)
    startDate.value = formatDate(start)
    endDate.value = formatDate(now)
    fetchStats()
  }
}

const handleCustomDateChange = val => {
  if (val && val.length === 2) {
    startDate.value = val[0]
    endDate.value = val[1]
    fetchStats()
  }
}

const formatDate = date => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const fetchStats = async () => {
  const params = { storeId: storeStore.currentStoreId }
  if (startDate.value) params.startDate = startDate.value
  if (endDate.value) params.endDate = endDate.value
  const res = await request.get('/stats/dashboard', { params })
  stats.value = res.data
  await nextTick()
  await nextTick()
  renderLineChart(res.data.last7Days || [])
  renderPieChart(res.data.categoryStats || [])
}

const renderLineChart = data => {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.date) },
    yAxis: { type: 'value', name: '销售额(元)' },
    series: [{
      name: '销售额', type: 'line', smooth: true,
      data: data.map(d => d.amount),
      itemStyle: { color: '#1890ff' },
      areaStyle: { color: 'rgba(24,144,255,0.1)' }
    }]
  })
}

const renderPieChart = data => {
  if (!pieChartRef.value || !data.length) return
  if (pieChart) { pieChart.dispose(); pieChart = null }
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['40%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: data.map(d => ({ name: d.categoryName, value: d.amount }))
    }]
  })
}

const initDefaultRange = () => {
  const now = new Date()
  const start = new Date(now)
  start.setDate(now.getDate() - 6)
  startDate.value = formatDate(start)
  endDate.value = formatDate(now)
}

watch(() => storeStore.currentStoreId, () => fetchStats())

onMounted(() => {
  initDefaultRange()
  fetchStats()
})
</script>

<style scoped>
.dashboard-page {
  padding: 2px 2px 24px;
}

/* ===== 页头 ===== */
.page-header {
  margin-bottom: 20px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px;
}
.page-date {
  font-size: 13px;
  color: #aaa;
}

/* ===== KPI 卡片 ===== */
.kpi-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: default;
}
.kpi-card:hover {
  box-shadow: 0 6px 16px rgba(0,0,0,0.09);
  transform: translateY(-2px);
}
.kpi-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.kpi-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
}
.kpi-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 4px;
}

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  margin-bottom: 20px;
}
.filter-label {
  font-size: 13px;
  color: #666;
  flex-shrink: 0;
}

/* ===== 图表卡片 ===== */
.chart-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f5f5f5;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

/* ===== 排行列表 ===== */
.rank-card :deep(.el-card__body) {
  padding: 12px 20px;
}
.rank-item {
  display: flex;
  align-items: center;
  padding: 11px 0;
  border-bottom: 1px solid #fafafa;
}
.rank-item:last-child { border-bottom: none; }
.rank-no {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #666;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
}
.rank-1 { background: #ff4d4f; color: #fff; }
.rank-2 { background: #fa8c16; color: #fff; }
.rank-3 { background: #fadb14; color: #555; }
.rank-name { flex: 1; margin-left: 12px; font-size: 14px; color: #333; }
.rank-amount { color: #1890ff; font-weight: 600; font-size: 14px; }

/* ===== AI 月报卡片 ===== */
.ai-report-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e0e0f0;
  box-shadow: 0 2px 12px rgba(99,102,241,0.12);
}

.ai-report-header {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  padding: 15px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ai-report-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}
.ai-month-badge {
  font-size: 12px;
  font-weight: 400;
  background: rgba(255,255,255,0.2);
  color: rgba(255,255,255,0.9);
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 4px;
}
.ai-report-actions {
  display: flex;
  gap: 8px;
}
.ai-btn {
  padding: 5px 14px;
  font-size: 13px;
  border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.35);
  background: rgba(255,255,255,0.15);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.2s;
}
.ai-btn:hover:not(:disabled) { background: rgba(255,255,255,0.28); }
.ai-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.ai-report-body {
  background: #fff;
  min-height: 260px;
  padding: 24px 24px;
  position: relative;
}

/* 空状态 */
.ai-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  gap: 10px;
  text-align: center;
}
.ai-empty-icon { color: #d0d0e8; }
.ai-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #888;
  margin: 0;
}
.ai-empty-desc {
  font-size: 13px;
  color: #aaa;
  margin: 0;
  line-height: 1.7;
  max-width: 360px;
}

/* 加载动画 */
.ai-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  gap: 16px;
  color: #7c7cf0;
  font-size: 14px;
}
.ai-dots { display: flex; gap: 7px; }
.ai-dots span {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #6366f1;
  animation: ai-bounce 1.2s ease-in-out infinite;
}
.ai-dots span:nth-child(2) { animation-delay: 0.2s; }
.ai-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes ai-bounce {
  0%, 80%, 100% { transform: scale(0.55); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* 报告正文 */
.ai-content {
  font-size: 14px;
  line-height: 1.9;
  color: #444;
}
.ai-content :deep(h2) {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 20px 0 8px;
  padding-left: 10px;
  border-left: 3px solid #6366f1;
  line-height: 1.4;
}
.ai-content :deep(h3) {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 14px 0 6px;
}
.ai-content :deep(p) {
  margin: 5px 0;
  color: #555;
}
.ai-content :deep(ul) {
  padding-left: 20px;
  margin: 8px 0;
}
.ai-content :deep(li) {
  margin: 4px 0;
  color: #555;
}
.ai-content :deep(strong) {
  color: #1a1a1a;
  font-weight: 600;
}

/* 打字光标 */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: #6366f1;
  margin-left: 2px;
  vertical-align: middle;
  animation: blink 0.8s step-end infinite;
}
@keyframes blink { 50% { opacity: 0; } }
</style>
