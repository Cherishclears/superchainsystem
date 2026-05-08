<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :model="searchForm" inline>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入姓名" clearable style="width:150px" />
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
          <span>会员列表</span>
          <el-button type="primary" @click="dialogVisible = true">
            <el-icon><Plus /></el-icon> 新增会员
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="memberNo" label="会员编号" width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="levelId" label="等级" width="90">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.levelId)">{{ levelName(row.levelId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="当前积分" width="100" />
        <el-table-column prop="totalAmount" label="累计消费" width="110">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
      </el-table>

      <div style="margin-top:16px; display:flex; justify-content:flex-end">
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

    <!-- 新增会员弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增会员" width="460px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="form.birthday" type="date" placeholder="选择生日" value-format="YYYY-MM-DD" style="width:100%" />
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
import { getMemberPage, registerMember } from '../../api/member'

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ phone: '', realName: '' })
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })

const levelName = id => ({ 1: '普通', 2: '银卡', 3: '金卡', 4: '钻石' }[id] || '普通')
const levelTagType = id => ({ 1: '', 2: 'info', 3: 'warning', 4: 'danger' }[id] || '')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMemberPage({
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
const handleReset = () => { searchForm.phone = ''; searchForm.realName = ''; handleSearch() }

// 新增会员
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ phone: '', realName: '', gender: 0, birthday: null, registerStore: 1 })
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const resetForm = () => {
  formRef.value?.resetFields()
  form.birthday = null
}

const handleSubmit = async () => {
  await formRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      await registerMember(form)
      ElMessage.success('注册成功')
      dialogVisible.value = false
      fetchData()
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => fetchData())
</script>
