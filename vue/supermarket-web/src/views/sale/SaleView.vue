<template>
  <div>

    <!-- 订单列表 -->
    <el-card shadow="never">
      <template #header>销售订单列表</template>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="totalAmount" label="商品总额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="discountAmount" label="优惠金额" width="100">
          <template #default="{ row }">¥{{ row.discountAmount }}</template>
        </el-table-column>
        <el-table-column prop="payableAmount" label="实付金额" width="100">
          <template #default="{ row }">¥{{ row.payableAmount }}</template>
        </el-table-column>
        <el-table-column prop="paymentType" label="支付方式" width="100">
          <template #default="{ row }">
            {{ { 1: '现金', 2: '微信', 3: '支付宝', 4: '会员卡' }[row.paymentType] }}
          </template>
        </el-table-column>
        <el-table-column prop="memberId" label="会员" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.memberId" type="success" size="small">会员</el-tag>
            <span v-else style="color:#999">非会员</span>
          </template>
        </el-table-column>
        <el-table-column prop="pointsEarned" label="获得积分" width="90" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '已完成' : '已退款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
                v-if="row.status === 1"
                size="small" type="danger"
                @click="handleReturn(row.orderNo)">退货</el-button>
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



    <!-- 退货弹窗 -->
    <el-dialog v-model="returnDialogVisible" title="退货" width="400px">
      <el-form label-width="90px">
        <el-form-item label="原订单号">
          <el-input v-model="returnOrderNo" disabled />
        </el-form-item>
        <el-form-item label="退货原因">
          <el-input v-model="returnReason" placeholder="请输入退货原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="returnLoading" @click="confirmReturn">确认退货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useStoreStore } from '../../stores/store'
const storeStore = useStoreStore()

import { ref, reactive, onMounted ,watch} from 'vue'
import { ElMessage } from 'element-plus'
import { getSalePage, returnOrder } from '../../api/sale'

const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSalePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      storeId: storeStore.currentStoreId
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

// 退货
const returnDialogVisible = ref(false)
const returnOrderNo = ref('')
const returnReason = ref('')
const returnLoading = ref(false)

const handleReturn = orderNo => {
  returnOrderNo.value = orderNo
  returnReason.value = ''
  returnDialogVisible.value = true
}

const confirmReturn = async () => {
  returnLoading.value = true
  try {
    await returnOrder(returnOrderNo.value, returnReason.value)
    ElMessage.success('退货成功')
    returnDialogVisible.value = false
    fetchData()
  } finally {
    returnLoading.value = false
  }
}

onMounted(() => fetchData())

watch(() => storeStore.currentStoreId, () => {
  fetchData()
})
</script>
