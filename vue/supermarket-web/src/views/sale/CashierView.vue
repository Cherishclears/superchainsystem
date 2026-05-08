<template>
  <div class="cashier-layout">
    <!-- 左侧：扫码区 + 购物车 -->
    <div class="cashier-left">
      <!-- 扫码输入 -->
      <el-card shadow="never" style="margin-bottom: 12px">
        <div style="display:flex; gap:8px; align-items:center">
          <el-input
            ref="barcodeInputRef"
            v-model="barcodeInput"
            placeholder="扫描条形码或输入商品编码，按回车添加"
            size="large"
            clearable
            @keyup.enter="handleScanBarcode"
            style="flex:1"
          >
            <template #prefix>
              <el-icon><Scan /></el-icon>
            </template>
          </el-input>
          <el-button size="large" type="primary" @click="handleScanBarcode">添加</el-button>
        </div>
      </el-card>

      <!-- 购物车列表 -->
      <el-card shadow="never" style="flex:1">
        <template #header>
          <div style="display:flex; justify-content:space-between; align-items:center">
            <span>购物车（{{ cartItems.length }}件）</span>
            <el-button size="small" type="danger" text @click="clearCart">清空</el-button>
          </div>
        </template>

        <el-empty v-if="cartItems.length === 0" description="请扫描商品条形码" />

        <div v-else class="cart-list">
          <div v-for="(item, index) in cartItems" :key="index" class="cart-item">
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.productName }}</div>
              <div class="cart-item-spec">{{ item.spec }} · ¥{{ item.unitPrice }}</div>
            </div>
            <div class="cart-item-controls">
              <el-button size="small" circle @click="decreaseQty(index)">
                <el-icon><Minus /></el-icon>
              </el-button>
              <el-input-number
                v-model="item.quantity"
                :min="0.001"
                :precision="item.isWeighable ? 3 : 0"
                :step="item.isWeighable ? 0.1 : 1"
                size="small"
                style="width: 100px"
                @change="val => updateQty(index, val)"
              />
              <el-button size="small" circle @click="increaseQty(index)">
                <el-icon><Plus /></el-icon>
              </el-button>
              <span class="cart-item-subtotal">¥{{ item.subtotal }}</span>
              <el-button size="small" type="danger" circle @click="removeItem(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 右侧：会员 + 结算 -->
    <div class="cashier-right">
      <!-- 会员识别 -->
      <el-card shadow="never" style="margin-bottom: 12px">
        <template #header>会员识别</template>
        <el-input
          v-model="memberPhone"
          placeholder="输入会员手机号按回车"
          clearable
          @keyup.enter="handleSearchMember"
          @clear="clearMember"
        >
          <template #append>
            <el-button @click="handleSearchMember">查询</el-button>
          </template>
        </el-input>

        <div v-if="currentMember" class="member-info">
          <el-tag type="success" size="large">
            {{ currentMember.memberName || '会员' }} ·
            {{ levelName(currentMember.levelId) }} ·
            积分{{ currentMember.currentPoints }}
          </el-tag>
          <div style="font-size:12px; color:#999; margin-top:4px">
            享受 {{ discountText }} 优惠
          </div>
        </div>
        <el-empty v-else-if="memberSearched" description="该手机号未注册会员" :image-size="50" />
      </el-card>

      <!-- 金额汇总 -->
      <el-card shadow="never" style="margin-bottom: 12px">
        <template #header>金额汇总</template>
        <div class="amount-row">
          <span>商品总额</span>
          <span>¥{{ totalAmount }}</span>
        </div>
        <div class="amount-row" v-if="discountAmount > 0">
          <span style="color:#52c41a">会员优惠</span>
          <span style="color:#52c41a">-¥{{ discountAmount }}</span>
        </div>
        <el-divider style="margin: 12px 0" />
        <div class="amount-row payable">
          <span>实付金额</span>
          <span class="payable-amount">¥{{ payableAmount }}</span>
        </div>
      </el-card>

      <!-- 找零计算 -->
      <el-card shadow="never" style="margin-bottom: 12px">
        <template #header>找零计算</template>
        <el-form inline>
          <el-form-item label="收款">
            <el-input-number
              v-model="receivedAmount"
              :min="0"
              :precision="2"
              style="width: 140px"
              @change="calcChange"
            />
          </el-form-item>
          <el-form-item label="找零">
            <span class="change-amount" :class="{ 'change-negative': changeAmount < 0 }">
              ¥{{ changeAmount >= 0 ? changeAmount.toFixed(2) : '收款不足' }}
            </span>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 支付方式 + 结账 -->
      <el-card shadow="never">
        <template #header>支付方式</template>
        <el-radio-group v-model="paymentType" style="margin-bottom: 16px">
          <el-radio-button :value="1">现金</el-radio-button>
          <el-radio-button :value="2">微信</el-radio-button>
          <el-radio-button :value="3">支付宝</el-radio-button>
          <el-radio-button :value="4">会员卡</el-radio-button>
        </el-radio-group>

        <el-button
          type="primary"
          size="large"
          style="width:100%"
          :loading="checkoutLoading"
          :disabled="cartItems.length === 0"
          @click="handleCheckout"
        >
          结账 ¥{{ payableAmount }}
        </el-button>
      </el-card>
    </div>

    <!-- 结账成功弹窗 -->
    <el-dialog v-model="successDialogVisible" title="结账成功" width="360px" center>
      <div class="success-dialog">
        <el-icon color="#52c41a" size="60"><CircleCheckFilled /></el-icon>
        <div class="success-amount">¥{{ lastPayableAmount }}</div>
        <div class="success-info">
          <div>订单号：{{ lastOrderNo }}</div>
          <div v-if="changeAmount > 0">找零：¥{{ changeAmount.toFixed(2) }}</div>
          <div v-if="currentMember">会员积分 +{{ earnedPoints }}</div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" size="large" style="width:100%" @click="handleNextOrder">
          下一单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getProductByBarcode } from '../../api/product'
import { getMemberByPhone } from '../../api/member'
import { createSaleOrder } from '../../api/sale'
import { useStoreStore } from '../../stores/store'

const storeStore = useStoreStore()

// ---- 扫码 ----
const barcodeInputRef = ref(null)
const barcodeInput = ref('')

const handleScanBarcode = async () => {
  const barcode = barcodeInput.value.trim()
  if (!barcode) return

  if (!storeStore.currentStoreId) {
    ElMessage.warning('请先在顶部选择门店')
    return
  }

  try {
    const res = await getProductByBarcode(barcode, storeStore.currentStoreId)
    addToCart(res.data)
    barcodeInput.value = ''
  } catch (e) {
    ElMessage.error('商品不存在或已停售')
  }
  await nextTick()
  barcodeInputRef.value?.focus()
}

// ---- 购物车 ----
const cartItems = ref([])

const addToCart = product => {
  const existing = cartItems.value.find(i => i.productId === product.id)
  if (existing && !product.isWeighable) {
    existing.quantity += 1
    updateSubtotal(existing)
  } else {
    cartItems.value.push({
      productId: product.id,
      productName: product.productName,
      spec: product.spec,
      barcode: product.barcode,
      unitPrice: product.retailPrice,
      memberPrice: product.memberPrice,
      isWeighable: product.isWeighable,
      quantity: 1,
      subtotal: product.retailPrice
    })
  }
  ElMessage.success({ message: `已添加：${product.productName}`, duration: 800 })
}

const updateSubtotal = item => {
  const price = currentMember.value && item.memberPrice ? item.memberPrice : item.unitPrice
  item.subtotal = (parseFloat(price) * item.quantity).toFixed(2)
}

const updateQty = (index, val) => {
  if (!val || val <= 0) {
    cartItems.value.splice(index, 1)
    return
  }
  cartItems.value[index].quantity = val
  updateSubtotal(cartItems.value[index])
}

const increaseQty = index => {
  cartItems.value[index].quantity += cartItems.value[index].isWeighable ? 0.1 : 1
  updateSubtotal(cartItems.value[index])
}

const decreaseQty = index => {
  const item = cartItems.value[index]
  const step = item.isWeighable ? 0.1 : 1
  if (item.quantity - step <= 0) {
    cartItems.value.splice(index, 1)
  } else {
    item.quantity = parseFloat((item.quantity - step).toFixed(3))
    updateSubtotal(item)
  }
}

const removeItem = index => cartItems.value.splice(index, 1)
const clearCart = () => {
  cartItems.value = []
  clearMember()
}

// ---- 会员 ----
const memberPhone = ref('')
const currentMember = ref(null)
const memberSearched = ref(false)

const levelName = id => ({ 1: '普通', 2: '银卡', 3: '金卡', 4: '钻石' }[id] || '')
const discountMap = { 1: 1, 2: 0.98, 3: 0.95, 4: 0.9 }

const discountText = computed(() => {
  if (!currentMember.value) return ''
  const rate = discountMap[currentMember.value.levelId] || 1
  return rate === 1 ? '无折扣' : `${rate * 10}折`
})

const handleSearchMember = async () => {
  if (!memberPhone.value) return
  memberSearched.value = true
  try {
    const res = await getMemberByPhone(memberPhone.value)
    currentMember.value = res.data
    // 更新购物车中有会员价的商品小计
    cartItems.value.forEach(item => updateSubtotal(item))
    ElMessage.success(`已识别会员：${res.data.memberName || res.data.phone}`)
  } catch (e) {
    currentMember.value = null
  }
}

const clearMember = () => {
  currentMember.value = null
  memberSearched.value = false
  memberPhone.value = ''
  cartItems.value.forEach(item => updateSubtotal(item))
}

// ---- 金额计算 ----
const totalAmount = computed(() => {
  return cartItems.value
    .reduce((sum, item) => sum + parseFloat(item.unitPrice) * item.quantity, 0)
    .toFixed(2)
})

const payableAmount = computed(() => {
  return cartItems.value
    .reduce((sum, item) => sum + parseFloat(item.subtotal), 0)
    .toFixed(2)
})

const discountAmount = computed(() => {
  return (parseFloat(totalAmount.value) - parseFloat(payableAmount.value)).toFixed(2)
})

// ---- 找零 ----
const receivedAmount = ref(0)
const changeAmount = computed(() => {
  return parseFloat((receivedAmount.value - parseFloat(payableAmount.value)).toFixed(2))
})
const calcChange = () => {}

// ---- 结账 ----
const paymentType = ref(1)
const checkoutLoading = ref(false)
const successDialogVisible = ref(false)
const lastOrderNo = ref('')
const lastPayableAmount = ref(0)
const earnedPoints = ref(0)

const handleCheckout = async () => {
  if (!storeStore.currentStoreId) {
    ElMessage.warning('请先选择门店')
    return
  }
  checkoutLoading.value = true
  try {
    const res = await createSaleOrder({
      storeId: storeStore.currentStoreId,
      cashierId: 1,
      memberPhone: currentMember.value ? memberPhone.value : undefined,
      paymentType: paymentType.value,
      items: cartItems.value.map(item => ({
        productId: item.productId,
        quantity: item.quantity
      }))
    })
    lastOrderNo.value = res.data.orderNo
    lastPayableAmount.value = payableAmount.value
    earnedPoints.value = res.data.pointsEarned || 0
    successDialogVisible.value = true
  } finally {
    checkoutLoading.value = false
  }
}

const handleNextOrder = () => {
  successDialogVisible.value = false
  clearCart()
  receivedAmount.value = 0
  paymentType.value = 1
  nextTick(() => barcodeInputRef.value?.focus())
}

onMounted(() => {
  nextTick(() => barcodeInputRef.value?.focus())
})
</script>

<style scoped>
.cashier-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 112px);
}
.cashier-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.cashier-right {
  width: 360px;
  flex-shrink: 0;
  overflow-y: auto;
}
.cart-list {
  overflow-y: auto;
  max-height: calc(100vh - 280px);
}
.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.cart-item:last-child { border-bottom: none; }
.cart-item-name { font-size: 15px; font-weight: 500; }
.cart-item-spec { font-size: 12px; color: #999; margin-top: 2px; }
.cart-item-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cart-item-subtotal {
  font-weight: 600;
  color: #1890ff;
  min-width: 70px;
  text-align: right;
}
.member-info { margin-top: 12px; }
.amount-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}
.amount-row.payable { font-size: 16px; font-weight: 600; }
.payable-amount { color: #ff4d4f; font-size: 22px; }
.change-amount { font-size: 18px; font-weight: 600; color: #52c41a; }
.change-negative { color: #ff4d4f; }
.success-dialog {
  text-align: center;
  padding: 16px 0;
}
.success-amount {
  font-size: 36px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 16px 0;
}
.success-info {
  color: #666;
  font-size: 14px;
  line-height: 2;
}
</style>
