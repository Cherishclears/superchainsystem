<template>
  <div>
    <!-- 门店选择 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form inline>
        <el-form-item label="选择门店">
          <el-select
            v-model="selectedStoreId"
            placeholder="请选择门店"
            style="width: 200px"
            @change="fetchData"
          >
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.storeName"
              :value="store.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedStoreId">
          <el-button type="primary" @click="handleBatchSave" :loading="saveLoading">
            保存所有配置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 提示 -->
    <el-alert
      v-if="!selectedStoreId"
      title="请先选择门店，然后为该门店配置商品售价和上下架状态"
      type="info"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 分类 Tag 筛选栏 -->
    <el-card shadow="never" style="margin-bottom: 16px" v-if="selectedStoreId">
      <div class="category-tags">
        <div class="tag-row">
          <span class="category-label">一级分类：</span>
          <el-check-tag
            :checked="selectedTop === null"
            @change="selectTopCategory(null)"
            style="margin-right: 8px"
          >全部</el-check-tag>
          <el-check-tag
            v-for="cat in topCategories"
            :key="cat.id"
            :checked="selectedTop === cat.id"
            @change="selectTopCategory(cat.id)"
            style="margin-right: 8px"
          >{{ cat.categoryName }}</el-check-tag>
        </div>
        <div class="tag-row" v-if="currentSubs.length > 0">
          <span class="category-label">二级分类：</span>
          <el-check-tag
            :checked="selectedCategoryId === null"
            @change="selectCategory(null)"
            style="margin-right: 8px"
          >全部</el-check-tag>
          <el-check-tag
            v-for="sub in currentSubs"
            :key="sub.id"
            :checked="selectedCategoryId === sub.id"
            @change="selectCategory(sub.id)"
            style="margin-right: 8px"
          >{{ sub.categoryName }}</el-check-tag>
        </div>
      </div>
    </el-card>

    <!-- 商品配置表格 -->
    <el-card shadow="never" v-if="selectedStoreId">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>商品配置 - {{ currentStoreName }}</span>
          <div style="display:flex; gap:8px">
            <el-button @click="handleEnableAll">全部启售</el-button>
            <el-button @click="handleDisableAll">全部停售</el-button>
            <el-button @click="handleResetAll">全部恢复默认价</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredTableData" v-loading="loading" stripe>
        <el-table-column prop="productName" label="商品名称" min-width="130" />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.categoryName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="spec" label="规格" width="90" />
        <el-table-column label="默认售价" width="100">
          <template #default="{ row }">¥{{ row.defaultRetailPrice }}</template>
        </el-table-column>
        <el-table-column label="门店售价" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.retailPrice"
              :precision="2"
              :min="0"
              size="small"
              :placeholder="'默认¥' + row.defaultRetailPrice"
              style="width: 130px"
            />
          </template>
        </el-table-column>
        <el-table-column label="默认会员价" width="110">
          <template #default="{ row }">
            {{ row.defaultMemberPrice ? '¥' + row.defaultMemberPrice : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="门店会员价" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.memberPrice"
              :precision="2"
              :min="0"
              size="small"
              :placeholder="row.defaultMemberPrice ? '默认¥' + row.defaultMemberPrice : '未设置'"
              style="width: 130px"
            />
          </template>
        </el-table-column>
        <el-table-column label="本店状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="在售"
              inactive-text="停售"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" @click="handleResetRow(row)">恢复默认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px; text-align:right">
        <el-button type="primary" size="large" :loading="saveLoading" @click="handleBatchSave">
          保存所有配置
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStoreList } from '../../api/store'
import { getProductPage } from '../../api/product'
import { getCategoryList } from '../../api/category'
import { getStoreProductList, batchSaveConfig } from '../../api/storeProduct'


const storeList = ref([])
const selectedStoreId = ref(null)
const loading = ref(false)
const saveLoading = ref(false)
const tableData = ref([])
const allCategories = ref([])






//



const selectedTop = ref(null)
const selectedCategoryId = ref(null)

const topCategories = computed(() =>
  allCategories.value.filter(c => c.parentId === 0)
)

const subCategoryMap = computed(() => {
  const map = {}
  allCategories.value.filter(c => c.parentId !== 0).forEach(c => {
    if (!map[c.parentId]) map[c.parentId] = []
    map[c.parentId].push(c)
  })
  return map
})

const currentSubs = computed(() => {
  if (!selectedTop.value) return []
  return subCategoryMap.value[selectedTop.value] || []
})

const selectTopCategory = id => {
  selectedTop.value = id
  selectedCategoryId.value = null
}

const selectCategory = id => {
  selectedCategoryId.value = id
}

// 表格数据根据分类筛选
const filteredTableData = computed(() => {
  if (!selectedCategoryId.value && !selectedTop.value) return tableData.value
  return tableData.value.filter(row => {
    if (selectedCategoryId.value) return row.categoryId === selectedCategoryId.value
    if (selectedTop.value) {
      const subs = subCategoryMap.value[selectedTop.value] || []
      const subIds = subs.map(s => s.id)
      return subIds.includes(row.categoryId) || row.categoryId === selectedTop.value
    }
    return true
  })
})



//



const currentStoreName = computed(() => {
  const store = storeList.value.find(s => s.id === selectedStoreId.value)
  return store ? store.storeName : ''
})

const fetchData = async () => {
  if (!selectedStoreId.value) return
  loading.value = true
  try {
    // 并行加载商品列表和门店配置
    const [prodRes, configRes] = await Promise.all([
      getProductPage({ pageNum: 1, pageSize: 200 }),
      getStoreProductList(selectedStoreId.value)
    ])

    const products = prodRes.data.records
    const configs = configRes.data

    // 构建配置Map
    const configMap = {}
    configs.forEach(c => { configMap[c.productId] = c })

    // 合并商品和配置
    tableData.value = products.map(p => {
      const config = configMap[p.id]
      const catName = allCategories.value.find(c => c.id === p.categoryId)?.categoryName || '-'
      return {
        productId: p.id,
        productName: p.productName,
        categoryName: catName,
        spec: p.spec,
        defaultRetailPrice: p.retailPrice,
        defaultMemberPrice: p.memberPrice,
        // 门店配置，没有配置就用默认值
        retailPrice: config?.retailPrice ?? null,
        memberPrice: config?.memberPrice ?? null,
        status: config?.status ?? 1
      }
    })
  } finally {
    loading.value = false
  }
}

// 全部启售
const handleEnableAll = () => {
  tableData.value.forEach(row => row.status = 1)
}

// 全部停售
const handleDisableAll = () => {
  tableData.value.forEach(row => row.status = 0)
}

// 全部恢复默认价
const handleResetAll = () => {
  tableData.value.forEach(row => {
    row.retailPrice = null
    row.memberPrice = null
  })
  ElMessage.success('已恢复所有商品默认价格')
}

// 单行恢复默认
const handleResetRow = row => {
  row.retailPrice = null
  row.memberPrice = null
  ElMessage.success(`${row.productName} 已恢复默认价格`)
}

// 保存所有配置
const handleBatchSave = async () => {
  saveLoading.value = true
  try {
    const configs = tableData.value.map(row => ({
      storeId: selectedStoreId.value,
      productId: row.productId,
      retailPrice: row.retailPrice,
      memberPrice: row.memberPrice,
      status: row.status
    }))
    await batchSaveConfig(configs)
    ElMessage.success('配置保存成功')
  } finally {
    saveLoading.value = false
  }
}

onMounted(async () => {
  const [storeRes, catRes] = await Promise.all([
    getStoreList(),
    getCategoryList()
  ])
  storeList.value = storeRes.data
  allCategories.value = catRes.data
})
</script>
<style scoped>
.category-tags {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}
.tag-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.category-label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
  margin-right: 4px;
  min-width: 70px;
}
</style>
