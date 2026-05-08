<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>门店列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增门店
          </el-button>
        </div>
      </template>

      <el-table :data="storeList" v-loading="loading" stripe>
        <el-table-column prop="storeCode" label="门店编码" width="120" />
        <el-table-column prop="storeName" label="门店名称" min-width="130" />
        <el-table-column prop="region" label="区域" width="100" />
        <el-table-column prop="address" label="地址" min-width="200" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '营业中' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="门店编码" prop="storeCode">
          <el-input v-model="form.storeCode" placeholder="如：S005" />
        </el-form-item>
        <el-form-item label="门店名称" prop="storeName">
          <el-input v-model="form.storeName" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="所属区域">
          <el-input v-model="form.region" placeholder="如：华北、华东" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">营业中</el-radio>
            <el-radio :value="0">已关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStoreList, addStore, updateStore } from '../../api/store'

const loading = ref(false)
const storeList = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStoreList()
    storeList.value = res.data
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增门店')
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  storeCode: '',
  storeName: '',
  region: '',
  address: '',
  phone: '',
  status: 1
})

const rules = {
  storeCode: [{ required: true, message: '请输入门店编码', trigger: 'blur' }],
  storeName: [{ required: true, message: '请输入门店名称', trigger: 'blur' }]
}

const handleAdd = () => {
  dialogTitle.value = '新增门店'
  form.id = null
  dialogVisible.value = true
}

const handleEdit = row => {
  dialogTitle.value = '编辑门店'
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
        await updateStore(form)
        ElMessage.success('修改成功')
      } else {
        await addStore(form)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      fetchData()
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => fetchData())
</script>
