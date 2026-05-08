<template>
  <div>
    <!-- 无门店提示 -->
    <el-alert
      v-if="!storeStore.currentStoreId"
      title="请在顶部选择门店查看该门店的商品"
      type="info"
      show-icon
      style="margin-bottom: 16px"
    />

    <template v-else>
      <!-- 分类 Tag 筛选栏 -->
      <el-card shadow="never" style="margin-bottom: 16px">
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
              :checked="searchForm.categoryId === null || searchForm.categoryId === selectedTop"
              @change="selectCategory(selectedTop)"
              style="margin-right: 8px"
            >全部</el-check-tag>
            <el-check-tag
              v-for="sub in currentSubs"
              :key="sub.id"
              :checked="searchForm.categoryId === sub.id"
              @change="selectCategory(sub.id)"
              style="margin-right: 8px"
            >{{ sub.categoryName }}</el-check-tag>
          </div>
        </div>
      </el-card>

      <!-- 搜索栏 -->
      <el-card shadow="never" style="margin-bottom: 16px">
        <el-form :model="searchForm" inline>
          <el-form-item label="商品名称">
            <el-input
              v-model="searchForm.productName"
              placeholder="请输入商品名称"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
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
            <div style="display:flex; align-items:center; gap:8px">
              <span>{{ currentStoreName }} · 在售商品</span>
              <el-tag v-if="searchForm.categoryId" type="success" closable @close="selectCategory(null)">
                {{ getCategoryName(searchForm.categoryId) }}
              </el-tag>
            </div>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon> 新增商品
            </el-button>
          </div>
        </template>

        <el-table :data="tableData" v-loading="loading" stripe>
          <el-table-column prop="productCode" label="商品编码" width="160" />
          <el-table-column prop="barcode" label="条形码" width="140" />
          <el-table-column prop="productName" label="商品名称" min-width="120" />
          <el-table-column label="分类" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ getCategoryName(row.categoryId) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="60" />
          <el-table-column prop="spec" label="规格" width="100" />
          <el-table-column prop="purchasePrice" label="进价" width="90">
            <template #default="{ row }">¥{{ row.purchasePrice }}</template>
          </el-table-column>
          <el-table-column prop="retailPrice" label="零售价" width="90">
            <template #default="{ row }">¥{{ row.retailPrice }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-popconfirm title="确定在本店下架该商品吗？" @confirm="handleOffShelf(row)">
                <template #reference>
                  <el-button size="small" type="warning">下架</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top: 16px; display:flex; justify-content:flex-end">
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="fetchData"
          />
        </div>
      </el-card>
    </template>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="条形码">
              <el-input v-model="form.barcode" placeholder="请输入条形码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
                <el-option-group
                  v-for="cat in topCategories"
                  :key="cat.id"
                  :label="cat.categoryName"
                >
                  <el-option
                    v-for="sub in subCategoryMap[cat.id]"
                    :key="sub.id"
                    :label="sub.categoryName"
                    :value="sub.id"
                  />
                </el-option-group>
                <el-option
                  v-for="cat in topCategories"
                  v-show="!subCategoryMap[cat.id]?.length"
                  :key="'top-' + cat.id"
                  :label="cat.categoryName"
                  :value="cat.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如：瓶、箱、kg" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="form.spec" placeholder="如：500ml" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="进价" prop="purchasePrice">
              <el-input-number v-model="form.purchasePrice" :precision="2" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="零售价" prop="retailPrice">
              <el-input-number v-model="form.retailPrice" :precision="2" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会员价">
              <el-input-number v-model="form.memberPrice" :precision="2" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保质期(天)">
              <el-input-number v-model="form.shelfLifeDays" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否称重">
              <el-switch v-model="form.isWeighable" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted, computed, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {
  getStoreProductPage, addProduct, updateProduct, updateProductStatus
} from '../../api/product'
import {getCategoryList} from '../../api/category'
import {saveStoreProductConfig} from '../../api/storeProduct'
import {useStoreStore} from '../../stores/store'
import {getStoreList} from '../../api/store'

const storeStore = useStoreStore()

// ---- 门店名称 ----
const storeList = ref([])
const currentStoreName = computed(() => {
  const store = storeList.value.find(s => s.id === storeStore.currentStoreId)
  return store ? store.storeName : ''
})

// ---- 分类数据 ----
const allCategories = ref([])
const selectedTop = ref(null)

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

const getCategoryName = id => {
  const cat = allCategories.value.find(c => c.id === id)
  return cat ? cat.categoryName : '-'
}

const selectTopCategory = id => {
  selectedTop.value = id
  searchForm.categoryId = id
  handleSearch()
}

const selectCategory = id => {
  searchForm.categoryId = id
  handleSearch()
}

// ---- 列表 ----
const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({productName: '', categoryId: null})
const pagination = reactive({pageNum: 1, pageSize: 20, total: 0})

const fetchData = async () => {
  if (!storeStore.currentStoreId) return
  loading.value = true
  try {
    const res = await getStoreProductPage(storeStore.currentStoreId, {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      productName: searchForm.productName || undefined,
      categoryId: searchForm.categoryId || undefined
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1;
  fetchData()
}
const handleReset = () => {
  searchForm.productName = ''
  searchForm.categoryId = null
  selectedTop.value = null
  handleSearch()
}

// ---- 本店下架 ----
const handleOffShelf = async row => {
  await saveStoreProductConfig({
    storeId: storeStore.currentStoreId,
    productId: row.id,
    status: 0
  })
  ElMessage.success('已在本店下架')
  fetchData()
}

// ---- 弹窗 ----
const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  productName: '',
  barcode: '',
  categoryId: null,
  unit: '',
  spec: '',
  purchasePrice: 0,
  retailPrice: 0,
  memberPrice: null,
  shelfLifeDays: null,
  isWeighable: 0
})

const rules = {
  productName: [{required: true, message: '请输入商品名称', trigger: 'blur'}],
  categoryId: [{required: true, message: '请选择分类', trigger: 'change'}],
  unit: [{required: true, message: '请输入单位', trigger: 'blur'}],
  purchasePrice: [{required: true, message: '请输入进价', trigger: 'blur'}],
  retailPrice: [{required: true, message: '请输入零售价', trigger: 'blur'}]
}

const handleAdd = () => {
  dialogTitle.value = '新增商品'
  form.id = null
  dialogVisible.value = true
}

const handleEdit = row => {
  dialogTitle.value = '编辑商品'
  Object.assign(form, row)
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
  form.id = null
}

const handleSubmit = async () => {
  await formRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (form.id) {
        await updateProduct(form)
        ElMessage.success('修改成功')
      } else {
        await addProduct(form)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      fetchData()
    } finally {
      submitLoading.value = false
    }
  })
}

watch(() => storeStore.currentStoreId, () => fetchData())

onMounted(async () => {
  const [catRes, storeRes] = await Promise.all([
    getCategoryList(),
    getStoreList()
  ])
  allCategories.value = catRes.data
  storeList.value = storeRes.data
  fetchData()
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
