<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :model="searchForm" inline>
        <el-form-item label="出库门店">
          <el-select v-model="searchForm.fromStoreId" placeholder="全部" clearable style="width:140px">
            <el-option v-for="s in storeList" :key="s.id" :label="s.storeName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库门店">
          <el-select v-model="searchForm.toStoreId" placeholder="全部" clearable style="width:140px">
            <el-option v-for="s in storeList" :key="s.id" :label="s.storeName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="草稿" :value="0" />
            <el-option label="待审核" :value="1" />
            <el-option label="已审核" :value="2" />
            <el-option label="已发货" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已拒收" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>调拨单列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新建调拨单
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="transferNo" label="调拨单号" width="200" />
        <el-table-column label="出库门店" width="130">
          <template #default="{ row }">{{ getStoreName(row.fromStoreId) }}</template>
        </el-table-column>
        <el-table-column label="入库门店" width="130">
          <template #default="{ row }">{{ getStoreName(row.toStoreId) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewItems(row)">明细</el-button>
            <el-button v-if="row.status === 0" size="small" type="primary"
                       @click="handleSubmit(row.id)">提交审核</el-button>
            <el-button v-if="row.status === 1 && userStore.isHQ" size="small" type="success"
                       @click="handleApprove(row.id)">审核通过</el-button>
            <el-button v-if="row.status === 2 && !userStore.isHQ && userStore.storeId === row.fromStoreId"
                       size="small" type="warning" @click="handleShip(row.id)">确认发货</el-button>
            <el-button v-if="row.status === 3 && !userStore.isHQ && userStore.storeId === row.toStoreId"
                       size="small" type="success" @click="handleReceive(row.id)">确认收货</el-button>
            <el-popconfirm v-if="row.status === 3 && !userStore.isHQ && userStore.storeId === row.toStoreId"
                           title="确定拒收吗？库存将回滚" @confirm="handleReject(row.id)">
              <template #reference>
                <el-button size="small" type="danger">拒收</el-button>
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

    <!-- 新建调拨单弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建调拨单" width="620px" @close="resetForm">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出库门店" prop="fromStoreId"
                          :rules="[{ required: true, message: '请选择出库门店' }]">
              <el-select v-model="form.fromStoreId" placeholder="请选择" style="width:100%">
                <el-option v-for="s in storeList" :key="s.id" :label="s.storeName" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库门店" prop="toStoreId"
                          :rules="[{ required: true, message: '请选择入库门店' }]">
              <el-select v-model="form.toStoreId" placeholder="请选择" style="width:100%">
                <el-option v-for="s in storeList" :key="s.id"
                           :label="s.storeName" :value="s.id"
                           :disabled="s.id === form.fromStoreId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>

        <div style="margin-bottom:8px; font-weight:600">调拨商品</div>
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
          <el-table-column label="商品名称" min-width="140">
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
          <el-table-column label="数量" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0.001"
                               :precision="3" size="small" style="width:120px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ $index }">
              <el-button size="small" type="danger" @click="form.items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button style="margin-top:8px"
                   @click="form.items.push({ productId: null, productName: '', quantity: 1, productLoading: false })">
          + 添加商品
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 明细弹窗 -->
    <el-dialog v-model="itemsDialogVisible" title="调拨明细" width="500px">
      <el-table :data="currentItems" border>
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="quantity" label="数量" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { useUserStore } from '../../stores/user'
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStoreList } from '../../api/store'
import { getProductById } from '../../api/product'
import {
  getTransferPage, createTransfer, submitTransfer,
  approveTransfer, shipTransfer, receiveTransfer,
  rejectTransfer, getTransferItems
} from '../../api/transfer'

const storeList = ref([])
const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ fromStoreId: null, toStoreId: null, status: null })
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })

const statusName = s => ({ 0: '草稿', 1: '待审核', 2: '已审核', 3: '已发货', 4: '已完成', 5: '已拒收' }[s])
const statusTagType = s => ({ 0: 'info', 1: 'warning', 2: 'primary', 3: '', 4: 'success', 5: 'danger' }[s])
const getStoreName = id => storeList.value.find(s => s.id === id)?.storeName || id

const userStore = useUserStore()

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTransferPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => {
  searchForm.fromStoreId = null
  searchForm.toStoreId = null
  searchForm.status = null
  handleSearch()
}

// 流程操作
const handleSubmit = async id => { await submitTransfer(id); ElMessage.success('提交成功'); fetchData() }
const handleApprove = async id => { await approveTransfer(id); ElMessage.success('审核通过'); fetchData() }
const handleShip = async id => { await shipTransfer(id); ElMessage.success('发货成功'); fetchData() }
const handleReceive = async id => { await receiveTransfer(id); ElMessage.success('收货成功'); fetchData() }
const handleReject = async id => { await rejectTransfer(id); ElMessage.success('已拒收'); fetchData() }

// 根据商品ID自动查询商品信息
const fetchProductInfo = async (productId, row) => {
  if (!productId) {
    row.productName = ''
    return
  }
  row.productLoading = true
  try {
    const res = await getProductById(productId)
    if (res.data) {
      row.productName = res.data.productName
    } else {
      row.productName = '未找到该商品'
    }
  } catch {
    row.productName = '未找到该商品'
  } finally {
    row.productLoading = false
  }
}

// 新建
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({
  fromStoreId: null,
  toStoreId: null,
  remark: '',
  items: [{ productId: null, productName: '', quantity: 1, productLoading: false }]
})

const handleAdd = () => { dialogVisible.value = true }
const resetForm = () => {
  form.fromStoreId = null
  form.toStoreId = null
  form.remark = ''
  form.items = [{ productId: null, productName: '', quantity: 1, productLoading: false }]
}

const handleCreate = async () => {
  if (!form.fromStoreId || !form.toStoreId) {
    ElMessage.warning('请选择出库和入库门店')
    return
  }
  if (form.fromStoreId === form.toStoreId) {
    ElMessage.warning('出库和入库门店不能相同')
    return
  }
  if (form.items.length === 0) {
    ElMessage.warning('请添加调拨商品')
    return
  }
  const invalidItem = form.items.find(i => !i.productId)
  if (invalidItem) {
    ElMessage.warning('请填写所有商品ID')
    return
  }
  submitLoading.value = true
  try {
    await createTransfer(form)
    ElMessage.success('调拨单创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

// 明细
const itemsDialogVisible = ref(false)
const currentItems = ref([])

const handleViewItems = async row => {
  const res = await getTransferItems(row.id)
  currentItems.value = res.data
  itemsDialogVisible.value = true
}

onMounted(async () => {
  const res = await getStoreList()
  storeList.value = res.data
  fetchData()
})
</script>
