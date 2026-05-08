import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getUserInfo } from '../api/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login/LoginView.vue')
    },
    {
      path: '/',
      component: () => import('../components/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {

          path: 'cashier',
          name: 'cashier',
          component: () => import('../views/sale/CashierView.vue')
        },
        {
          path: 'store',
          name: 'store',
          component: () => import('../views/store/StoreView.vue')
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/dashboard/DashboardView.vue')
        },
        {
          path: 'product',
          name: 'product',
          component: () => import('../views/product/ProductView.vue')
        },
        {
          path: 'inventory',
          name: 'inventory',
          component: () => import('../views/inventory/InventoryView.vue')
        },
        {
          path: 'purchase',
          name: 'purchase',
          component: () => import('../views/purchase/PurchaseView.vue')
        },
        {
          path: 'sale',
          name: 'sale',
          component: () => import('../views/sale/SaleView.vue')
        },
        {
          path: 'member',
          name: 'member',
          component: () => import('../views/member/MemberView.vue')
        },
        {
          path: 'transfer',
          name: 'transfer',
          component: () => import('../views/inventory/TransferView.vue')
        },
        {
          path: 'store-product',
          name: 'store-product',
          component: () => import('../views/product/StoreProductView.vue')
        },
        {
          path: 'user',
          name: 'user',
          component: () => import('../views/user/UserView.vue')
        }
      ]
    },
    // 未匹配的路径重定向到首页
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
})

// 首次导航时验证 token 是否仍有效
let tokenVerified = false

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (!tokenVerified) {
    tokenVerified = true
    if (userStore.token) {
      try {
        await getUserInfo()
      } catch {
        // token 已失效，清除并跳转登录
        userStore.token = ''
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('permissions')
        return next('/login')
      }
    }
  }

  if (to.path === '/login') {
    if (userStore.isLoggedIn) return next('/')
    return next()
  }

  if (!userStore.isLoggedIn) return next('/login')

  // 收银员只能访问这三个页面
  const cashierAllowed = ['/cashier', '/sale', '/member', '/user']
  if (userStore.isCashier && !cashierAllowed.includes(to.path)) {
    return next('/cashier')
  }

  // 收银员登录后默认跳转到收银台
  if (to.path === '/' && userStore.isCashier) {
    return next('/cashier')
  }

  if (to.path === '/') return next('/dashboard')

  next()
})

export default router
