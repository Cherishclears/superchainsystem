<template>
  <div>
    <!-- 预警提示 -->
    <el-alert
      v-if="warningList.length > 0"
      :title="`当前有 ${warningList.length} 个商品库存低于预警线，请及时补货！`"
      type="warning"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 操作栏 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form inline>
        <el-form-item>
          <el-button type="primary" @click="openDialog('inbound')">入库</el-button>
          <el-button type="warning" @click="openDialog('outbound')">出库</el-button>
          <el-button type="info" @click="openDialog('adjust')">盘点调整</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 库存列表 -->
    <el-card shadow="never">
      <template #header>库存列表（门店：{{ storeStore.currentStoreId ? currentStoreName : '-' }}）</template>
      <el-table :data="inventoryList" v-loading="loading" stripe>
        <el-table-column prop="productId" label="商品ID" width="90" />
        <el-table-column label="商品名称" min-width="150">
          <template #default="{ row }">
            {{ productMap[row.productId] || '商品ID:' + row.productId }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="当前库存" width="110" />
        <el-table-column prop="warningQty" label="预警下限" width="110" />
        <el-table-column label="库存状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.quantity <= row.warningQty && row.warningQty > 0 ? 'danger' : 'success'"
            >
              {{ row.quantity <= row.warningQty && row.warningQty > 0 ? '库存不足' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="openWarningDialog(row)">
              设置预警
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 入库/出库/调整弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="商品ID" prop="productId">
          <el-input-number v-model="form.productId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item :label="dialogType === 'adjust' ? '调整后数量' : '数量'" prop="qty">
          <el-input-number v-model="form.qty" :min="0.001" :precision="3" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 设置预警下限弹窗 -->
    <el-dialog v-model="warningDialogVisible" title="设置预警下限" width="380px">
      <div style="margin-bottom: 16px; color: #666; font-size: 14px">
        <span>商品：</span>
        <strong>{{ productMap[warningForm.productId] || '商品ID:' + warningForm.productId }}</strong>
      </div>
      <el-form label-width="100px">
        <el-form-item label="当前库存">
          <span style="font-size: 15px; font-weight: 600; color: #1890ff">
            {{ warningForm.currentQty }}
          </span>
        </el-form-item>
        <el-form-item label="预警下限">
          <el-input-number
            v-model="warningForm.warningQty"
            :min="0"
            :precision="3"
            style="width: 200px"
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px">
            库存低于此值时系统将发出预警提示，设为 0 表示不预警
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warningDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="warningLoading" @click="handleWarningSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useStoreStore } from '../../stores/store'
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getInventoryList, getWarningList, inbound, outbound, adjust, updateWarningQty } from '../../api/inventory'
import { getProductPage } from '../../api/product'
import { getStoreList } from '../../api/store'

const storeStore = useStoreStore()
const loading = ref(false)
const inventoryList = ref([])
const warningList = ref([])
const productMap = ref({})

// 门店名称显示
const storeList = ref([])
const currentStoreName = computed(() => {
  const store = storeList.value.find(s => s.id === storeStore.currentStoreId)
  return store ? store.storeName : '-'
})

const fetchData = async () => {
  const storeId = storeStore.currentStoreId
  if (!storeId) {
    inventoryList.value = []
    warningList.value = []
    return
  }
  loading.value = true
  try {
    const [invRes, warnRes] = await Promise.all([
      getInventoryList(storeId),
      getWarningList(storeId)
    ])
    inventoryList.value = invRes.data
    warningList.value = warnRes.data

    // 加载商品名称映射
    const prodRes = await getProductPage({ pageNum: 1, pageSize: 100 })
    prodRes.data.records.forEach(p => {
      productMap.value[p.id] = p.productName
    })
  } finally {
    loading.value = false
  }
}

// ---- 入库/出库/调整弹窗 ----
const dialogVisible = ref(false)
const dialogType = ref('inbound')
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ productId: 1, qty: 1, remark: '' })

const dialogTitleMap = { inbound: '入库', outbound: '出库', adjust: '盘点调整' }
const dialogTitle = ref('入库')

const openDialog = type => {
  dialogType.value = type
  dialogTitle.value = dialogTitleMap[type]
  form.productId = 1
  form.qty = 1
  form.remark = ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const storeId = storeStore.currentStoreId
  if (!storeId) {
    ElMessage.error('请选择门店后再进行操作')
    return
  }
  submitLoading.value = true
  try {
    const params = { storeId, productId: form.productId }
    if (dialogType.value === 'adjust') {
      await adjust({ ...params, newQty: form.qty, remark: form.remark })
    } else if (dialogType.value === 'inbound') {
      await inbound({ ...params, qty: form.qty })
    } else {
      await outbound({ ...params, qty: form.qty })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

// ---- 设置预警下限弹窗 ----
const warningDialogVisible = ref(false)
const warningLoading = ref(false)
const warningForm = reactive({
  productId: null,
  currentQty: 0,
  warningQty: 0
})

const openWarningDialog = row => {
  warningForm.productId = row.productId
  warningForm.currentQty = row.quantity
  warningForm.warningQty = Number(row.warningQty) || 0
  warningDialogVisible.value = true
}

const handleWarningSubmit = async () => {
  const storeId = storeStore.currentStoreId
  if (!storeId) {
    ElMessage.error('请先选择门店')
    return
  }
  warningLoading.value = true
  try {
    await updateWarningQty({
      storeId,
      productId: warningForm.productId,
      warningQty: warningForm.warningQty
    })
    ElMessage.success('预警下限设置成功')
    warningDialogVisible.value = false
    fetchData()
  } finally {
    warningLoading.value = false
  }
}

onMounted(async () => {
  const res = await getStoreList()
  storeList.value = res.data
  fetchData()
})

watch(
  () => storeStore.currentStoreId,
  newId => {
    if (newId) fetchData()
    else {
      inventoryList.value = []
      warningList.value = []
    }
  }
)
</script>
