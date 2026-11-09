import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { UserInfo } from '@/types'
import { clearAuth, loadUser, saveAuth, TOKEN_KEY } from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const userInfo = ref<UserInfo | null>(loadUser())

  const isOperator = computed(() => userInfo.value?.roleCode === 'OPERATOR')
  const realName = computed(() => userInfo.value?.realName ?? '')

  function setAuth(newToken: string, user: UserInfo) {
    token.value = newToken
    userInfo.value = user
    saveAuth(newToken, user)
  }

  function clear() {
    token.value = ''
    userInfo.value = null
    clearAuth()
  }

  return { token, userInfo, isOperator, realName, setAuth, clear }
})
