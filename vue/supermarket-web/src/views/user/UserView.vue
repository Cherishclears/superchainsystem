<template>
  <div class="user-container">
    <!-- 个人信息卡片 -->
    <el-card class="profile-card" shadow="hover">
      <template #header>
        <span>个人信息</span>
      </template>
      <el-form :model="profileForm" label-width="80px" style="max-width: 480px">
        <el-form-item label="用户名">
          <el-input :model-value="userStore.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="roleTagType">{{ roleLabel }}</el-tag>
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdateProfile" :loading="profileLoading">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密码卡片 -->
    <el-card class="password-card" shadow="hover" style="margin-top: 16px">
      <template #header>
        <span>修改密码</span>
      </template>
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px" style="max-width: 480px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdatePassword" :loading="passwordLoading">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户管理表格（仅总部可见） -->
    <el-card v-if="userStore.isHQ" class="user-list-card" shadow="hover" style="margin-top: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>账号管理</span>
          <el-button type="primary" @click="showCreateDialog">新增账号</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" style="margin-bottom: 16px">
        <el-form-item label="用户名">
          <el-input v-model="searchUsername" placeholder="搜索用户名" clearable @clear="loadUsers" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchUserType" placeholder="全部" clearable @change="loadUsers">
            <el-option label="总部管理员" :value="1" />
            <el-option label="门店管理员" :value="2" />
            <el-option label="收银员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="userList" border stripe v-loading="tableLoading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getUserTypeTag(row.userType)">{{ getUserTypeLabel(row.userType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storeId" label="所属门店" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadUsers"
        @current-change="loadUsers"
      />
    </el-card>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增账号" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="createForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="userType">
          <el-select v-model="createForm.userType" placeholder="请选择角色">
            <el-option label="总部管理员" :value="1" />
            <el-option label="门店管理员" :value="2" />
            <el-option label="收银员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属门店" v-if="createForm.userType && createForm.userType !== 1" prop="storeId">
          <el-select v-model="createForm.storeId" placeholder="请选择门店">
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.storeName"
              :value="store.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateUser" :loading="createLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { getUserInfo, updateUserInfo, updatePassword, getUserPage, createUser, updateUserStatus, resetUserPassword } from '../../api/user'
import { getStoreList } from '../../api/store'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

// ---- 角色相关 ----
const roleLabel = computed(() => getUserTypeLabel(userStore.userType))
const roleTagType = computed(() => getUserTypeTag(userStore.userType))

function getUserTypeLabel(type) {
  const map = { 1: '总部管理员', 2: '门店管理员', 3: '收银员' }
  return map[type] || '未知'
}
function getUserTypeTag(type) {
  const map = { 1: 'danger', 2: '', 3: 'info' }
  return map[type] || ''
}

// ---- 个人信息 ----
const profileForm = ref({ realName: '', phone: '', avatar: '' })
const profileLoading = ref(false)

async function loadProfile() {
  const res = await getUserInfo()
  profileForm.value.realName = res.data.realName || ''
  profileForm.value.phone = res.data.phone || ''
  profileForm.value.avatar = res.data.avatar || ''
}

async function handleUpdateProfile() {
  profileLoading.value = true
  try {
    await updateUserInfo(profileForm.value)
    ElMessage.success('个人信息修改成功')
    // 同步更新 store 中的 realName
    userStore.userInfo.realName = profileForm.value.realName
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
  } finally {
    profileLoading.value = false
  }
}

// ---- 修改密码 ----
const passwordFormRef = ref(null)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordLoading = ref(false)

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleUpdatePassword() {
  await passwordFormRef.value.validate()
  passwordLoading.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    await userStore.logoutAction()
  } finally {
    passwordLoading.value = false
  }
}

// ---- 用户列表（总部） ----
const userList = ref([])
const tableLoading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const searchUsername = ref('')
const searchUserType = ref(null)

async function loadUsers() {
  if (!userStore.isHQ) return
  tableLoading.value = true
  try {
    const res = await getUserPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: searchUsername.value || undefined,
      userType: searchUserType.value || undefined
    })
    userList.value = res.data.records
    total.value = res.data.total
  } finally {
    tableLoading.value = false
  }
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定要${action}用户 ${row.username} 吗？`, '提示', { type: 'warning' })
  await updateUserStatus(row.id, newStatus)
  ElMessage.success(`已${action}`)
  loadUsers()
}

async function handleResetPassword(row) {
  await ElMessageBox.confirm(`确定要重置用户 ${row.username} 的密码为 123456 吗？`, '提示', { type: 'warning' })
  await resetUserPassword(row.id)
  ElMessage.success('密码已重置为 123456')
}

// ---- 新增用户 ----
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createLoading = ref(false)
const createForm = ref({ username: '', password: '', realName: '', phone: '', userType: null, storeId: null })
const storeList = ref([])

const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  userType: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function showCreateDialog() {
  createForm.value = { username: '', password: '', realName: '', phone: '', userType: null, storeId: null }
  createDialogVisible.value = true
}

async function handleCreateUser() {
  await createFormRef.value.validate()
  if (createForm.value.userType !== 1 && !createForm.value.storeId) {
    ElMessage.warning('非总部用户需选择所属门店')
    return
  }
  createLoading.value = true
  try {
    await createUser(createForm.value)
    ElMessage.success('账号创建成功')
    createDialogVisible.value = false
    loadUsers()
  } finally {
    createLoading.value = false
  }
}

// ---- 初始化 ----
onMounted(async () => {
  await loadProfile()
  if (userStore.isHQ) {
    loadUsers()
    const res = await getStoreList()
    storeList.value = res.data
  }
})
</script>

<style scoped>
.user-container {
  max-width: 1200px;
}
</style>
