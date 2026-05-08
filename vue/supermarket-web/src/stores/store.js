import { defineStore } from 'pinia'

export const useStoreStore = defineStore('storeContext', {
  state: () => ({
    currentStoreId: null,      // null 表示总部查看全部
    currentStoreName: ''
  }),

  actions: {
    setCurrentStore(storeId, storeName = '') {
      this.currentStoreId = storeId
      this.currentStoreName = storeName
    }
  },

  persist: false
})
