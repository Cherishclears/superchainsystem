<template>
  <el-container class="layout-container">

    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span>🏪 超市管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#ffffff"
      >
        <!-- 总部 + 店长可见 -->
        <template v-if="!userStore.isCashier">
          <el-menu-item index="/dashboard">
            <el-icon><DataLine /></el-icon>
            <span>数据看板</span>
          </el-menu-item>
          <el-menu-item index="/store" v-if="userStore.isHQ">
            <el-icon><OfficeBuilding /></el-icon>
            <span>门店管理</span>
          </el-menu-item>
          <el-menu-item index="/product">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/inventory">
            <el-icon><Box /></el-icon>
            <span>库存管理</span>
          </el-menu-item>
          <el-menu-item index="/transfer">
            <el-icon><Sort /></el-icon>
            <span>门店调拨</span>
          </el-menu-item>
          <el-menu-item index="/purchase">
            <el-icon><ShoppingCart /></el-icon>
            <span>采购管理</span>
          </el-menu-item>
          <el-menu-item index="/store-product" v-if="userStore.isHQ">
            <el-icon><Setting /></el-icon>
            <span>门店商品配置</span>
          </el-menu-item>
        </template>

        <!-- 所有角色可见 -->
        <el-menu-item index="/sale">
          <el-icon><Ticket /></el-icon>
          <span>销售管理</span>
        </el-menu-item>
        <el-menu-item index="/cashier">
          <el-icon><Monitor /></el-icon>
          <span>收银台</span>
        </el-menu-item>
        <el-menu-item index="/member">
          <el-icon><User /></el-icon>
          <span>会员管理</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><Avatar /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <!-- 总部账号显示门店切换器 -->
          <template v-if="userStore.isHQ">
            <el-select
              v-model="currentStoreId"
              placeholder="选择查看门店"
              style="width: 160px; margin-right: 16px"
              @change="handleStoreChange"
            >
              <el-option label="全部门店" :value="null" />
              <el-option
                v-for="store in storeList"
                :key="store.id"
                :label="store.storeName"
                :value="store.id"
              />
            </el-select>
          </template>

          <!-- 当前门店标签（门店账号显示） -->
          <template v-else>
            <el-tag type="success" style="margin-right: 16px">
              {{ currentStoreName }}
            </el-tag>
          </template>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar size="small" icon="UserFilled" />
              <span style="margin-left: 8px">{{ userStore.realName || userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

  </el-container>
</template>

<script setup>

import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useStoreStore } from '../stores/store'
import { ElMessageBox } from 'element-plus'
import { getStoreList } from '../api/store'

const route = useRoute()
const userStore = useUserStore()
const storeStore = useStoreStore()

const storeList = ref([])
const currentStoreId = ref(userStore.isHQ ? null : userStore.storeId)

const activeMenu = computed(() => route.path)

const pageTitleMap = {
  '/dashboard': '数据看板',
  '/store': '门店管理',
  '/product': '商品管理',
  '/inventory': '库存管理',
  '/purchase': '采购管理',
  '/sale': '销售管理',
  '/member': '会员管理',
  '/user': '用户管理'
}

const pageTitle = computed(() => pageTitleMap[route.path] || '')

const currentStoreName = computed(() => {
  const store = storeList.value.find(s => s.id === currentStoreId.value)
  return store ? store.storeName : '未知门店'
})

const handleStoreChange = storeId => {
  storeStore.setCurrentStore(storeId)
}

const handleCommand = async command => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logoutAction()
  }
}

onMounted(async () => {
  const res = await getStoreList()
  storeList.value = res.data
  // 初始化当前门店
  if (userStore.isHQ) {
    storeStore.setCurrentStore(null)
  } else {
    storeStore.setCurrentStore(userStore.storeId)
    currentStoreId.value = userStore.storeId
  }
})
</script>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: #001529; overflow: hidden; }
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #ffffff1a;
}
.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.page-title { font-size: 16px; font-weight: 600; color: #1a1a1a; }
.user-info { display: flex; align-items: center; cursor: pointer; color: #333; }
.main { background: #f5f7fa; padding: 24px; }
</style>
