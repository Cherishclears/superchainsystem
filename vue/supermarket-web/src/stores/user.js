import { defineStore } from 'pinia'
import { login, logout } from '../api/auth'
import router from '../router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]')
  }),

  getters: {
    isLoggedIn: state => !!state.token,
    username: state => state.userInfo.username || '',
    realName: state => state.userInfo.realName || '',
    userType: state => state.userInfo.userType,
    storeId: state => state.userInfo.storeId,
    isHQ: state => state.userInfo.userType === 1,
    isManager: state => state.userInfo.userType === 2,
    isCashier: state => state.userInfo.userType === 3
  },

  actions: {
    async loginAction(username, password) {
      const res = await login({ username, password })
      this.token = res.data.token
      this.userInfo = {
        userId: res.data.userId,
        username: res.data.username,
        realName: res.data.realName,
        avatar: res.data.avatar,
        storeId: res.data.storeId,
        userType: res.data.userType
      }
      this.permissions = res.data.permissions || []
      localStorage.setItem('token', this.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
    },

    async logoutAction() {
      try {
        await logout()
      } finally {
        this.token = ''
        this.userInfo = {}
        this.permissions = []
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('permissions')
        router.push('/login')
      }
    }
  }
})
