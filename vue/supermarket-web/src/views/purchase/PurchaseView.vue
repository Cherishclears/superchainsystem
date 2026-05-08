<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :model="searchForm" inline>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:130px">
            <el-option label="草稿" :value="0" />
            <el-option label="待审核" :value="1" />
            <el-option label="已审核" :value="2" />
            <el-option label="已入库" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>采购单列表</span>
          <div style="display:flex; gap:8px">
            <!-- AI 智能补货按钮 -->
            <el-tooltip
              :content="!storeStore.currentStoreId ? '请先在顶部选择具体门店' : 'AI 分析库存和销售数据，自动生成补货建议'"
              placement="top"
            >
              <el-button
                type="success"
                :disabled="!storeStore.currentStoreId"
                :loading="aiLoading"
                @click="handleAiSuggest"
              >
                <el-icon style="margin-right:4px"><MagicStick /></el-icon>
                AI 智能补货
              </el-button>
            </el-tooltip>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon> 新建采购单
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="采购单号" width="180" />
        <el-table-column prop="storeId" label="门店ID" width="80" />
        <el-table-column prop="supplierId" label="供应商ID" width="90" />
        <el-table-column prop="totalAmount" label="总金额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectedDate" label="预计到货" width="120" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="primary"
                       @click="handleSubmit(row.id)">提交审核</el-button>
            <el-button v-if="row.status === 1" size="small" type="success"
                       @click="handleApprove(row.id)">审核通过</el-button>
            <el-button v-if="row.status === 2" size="small" type="warning"
                       @click="handleReceive(row.id)">确认入库</el-button>
            <el-popconfirm v-if="row.status < 3" title="确定取消该采购单吗？"
                           @confirm="handleCancel(row.id)">
              <template #reference>
                <el-button size="small" type="danger">取消</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px; display:flex; justify-content:flex-end">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新建采购单弹窗（普通 + AI 预填共用） -->
    <el-dialog v-model="dialogVisible" title="新建采购单" width="720px" @close="resetForm">
      <!-- AI 预填提示条 -->
      <el-alert
        v-if="isAiFilled"
        type="success"
        show-icon
        style="margin-bottom: 16px"
        :closable="false"
      >
        <template #title>
          <span>
            ✨ AI 已根据库存和近7天销售数据预填了 <strong>{{ form.items.length }}</strong> 个商品的补货建议，
            请检查数量后确认提交。
          </span>
        </template>
      </el-alert>

      <el-form ref="formRef" :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商ID" prop="supplierId">
              <el-input-number v-model="form.supplierId" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计到货">
              <el-date-picker
                v-model="form.expectedDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 采购明细 -->
        <div style="margin-bottom:8px; font-weight:600; display:flex; align-items:center; gap:8px">
          <span>采购明细</span>
          <el-tag v-if="isAiFilled" type="success" size="small">AI 预填</el-tag>
        </div>

        <el-table :data="form.items" border size="small">
          <el-table-column label="商品ID" width="130">
            <template #default="{ row }">
              <el-input-number
                v-model="row.productId"
                :min="1"
                size="small"
                style="width:110px"
                @change="val => fetchProductInfo(val, row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="商品名称" min-width="130">
            <template #default="{ row }">
              <span v-if="row.productLoading" style="color:#999; font-size:12px">
                <el-icon class="is-loading"><Loading /></el-icon> 查询中...
              </span>
              <span v-else-if="row.productName" style="font-size:13px; color:#333">
                {{ row.productName }}
              </span>
              <span v-else style="color:#bbb; font-size:12px">输入ID后自动填充</span>
            </template>
          </el-table-column>
          <el-table-column label="采购价" width="130">
            <template #default="{ row }">
              <el-input-number
                v-model="row.purchasePrice"
                :min="0" :precision="2"
                size="small" style="width:110px"
              />
            </template>
          </el-table-column>
          <el-table-column label="采购数量" width="140">
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="0.001" :precision="3"
                size="small" style="width:120px"
              />
            </template>
          </el-table-column>
          <!-- AI 预填时额外显示参考列 -->
          <el-table-column v-if="isAiFilled" label="AI 补货原因" min-width="130">
            <template #default="{ row }">
              <el-tooltip :content="row.reason" placement="top">
                <span style="font-size:12px; color:#52c41a; cursor:default">
                  {{ row.reason || '-' }}
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ $index }">
              <el-button size="small" type="danger" @click="removeItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button style="margin-top:8px" @click="addItem">+ 手动添加商品</el-button>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreate">
          确认提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useStoreStore } from '../../stores/store'
const storeStore = useStoreStore()

import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getProductById } from '../../api/product'
import {
  getPurchasePage, createPurchase,
  submitPurchase, approvePurchase,
  receivePurchase, cancelPurchase,
  getAiSuggest
} from '../../api/purchase'

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ status: null })
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })

const statusName = s => ({ 0: '草稿', 1: '待审核', 2: '已审核', 3: '已入库', 4: '已取消' }[s] || '')
const statusTagType = s => ({ 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'danger' }[s] || '')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPurchasePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      storeId: storeStore.currentStoreId,
      ...searchForm
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => { searchForm.status = null; handleSearch() }

// 流程操作
const handleSubmit = async id => { await submitPurchase(id); ElMessage.success('提交成功'); fetchData() }
const handleApprove = async id => { await approvePurchase(id); ElMessage.success('审核通过'); fetchData() }
const handleReceive = async id => { await receivePurchase(id); ElMessage.success('入库成功'); fetchData() }
const handleCancel = async id => { await cancelPurchase(id); ElMessage.success('已取消'); fetchData() }

// ---- 根据商品ID自动查询商品信息 ----
const fetchProductInfo = async (productId, row) => {
  if (!productId) { row.productName = ''; return }
  row.productLoading = true
  try {
    const res = await getProductById(productId)
    if (res.data) {
      row.productName = res.data.productName
      if (!row.purchasePrice || row.purchasePrice === 0) {
        row.purchasePrice = res.data.purchasePrice || 0
      }
    } else {
      row.productName = '未找到该商品'
    }
  } catch {
    row.productName = '未找到该商品'
  } finally {
    row.productLoading = false
  }
}

// ---- 弹窗状态 ----
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isAiFilled = ref(false)   // 是否是 AI 预填状态
const formRef = ref(null)

const newEmptyItem = () => ({
  productId: null,
  productName: '',
  productLoading: false,
  purchasePrice: 0,
  quantity: 1,
  reason: ''
})

const form = reactive({
  supplierId: 1,
  expectedDate: '',
  remark: '',
  items: [newEmptyItem()]
})

// 打开普通新建
const openCreateDialog = () => {
  isAiFilled.value = false
  resetForm()
  dialogVisible.value = true
}

const addItem = () => form.items.push(newEmptyItem())
const removeItem = index => form.items.splice(index, 1)

const resetForm = () => {
  isAiFilled.value = false
  form.supplierId = 1
  form.expectedDate = ''
  form.remark = ''
  form.items = [newEmptyItem()]
}

const handleCreate = async () => {
  const invalidItem = form.items.find(i => !i.productId)
  if (invalidItem) { ElMessage.warning('请填写所有商品ID'); return }
  submitLoading.value = true
  try {
    await createPurchase({ ...form, storeId: storeStore.currentStoreId })
    ElMessage.success('采购单创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

// ---- AI 智能补货 ----
const aiLoading = ref(false)

const handleAiSuggest = async () => {
  if (!storeStore.currentStoreId) {
    ElMessage.warning('请先在顶部选择具体门店')
    return
  }
  aiLoading.value = true
  try {
    const res = await getAiSuggest(storeStore.currentStoreId)
    const suggestList = res.data

    if (!suggestList || suggestList.length === 0) {
      ElMessage.success('AI 分析完成：当前库存充足，暂无需要补货的商品 🎉')
      return
    }

    // 将 AI 建议填充到表单
    isAiFilled.value = true
    form.remark = 'AI智能补货+人工审核'
    form.items = suggestList.map(item => ({
      productId: item.productId,
      productName: item.productName,
      productLoading: false,
      purchasePrice: 0,   // 采购价需要采购员手动填写
      quantity: Number(item.suggestQty),
      reason: item.reason || ''
    }))

    // 批量查询采购价（自动填入进价）
    form.items.forEach(row => {
      if (row.productId) fetchProductInfo(row.productId, row)
    })

    dialogVisible.value = true
    ElMessage.success(`AI 分析完成，已预填 ${suggestList.length} 个商品的补货建议，请检查后提交`)
  } catch (e) {
    ElMessage.error(e.message || 'AI 分析失败，请稍后重试')
  } finally {
    aiLoading.value = false
  }
}

onMounted(() => fetchData())

watch(() => storeStore.currentStoreId, () => fetchData())
</script>
