import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

// 用户状态仓库：统一管理登录 token，并开启持久化（刷新页面不丢失）
export const useUserStore = defineStore('user', () => {
  // token：登录成功后由接口返回，用于请求拦截器携带到后端
  const token = ref('')
  const tokenName = ref('Authorization')
  const roleCode = ref('')

  const decodeJwtPayload = (jwtToken) => {
    if (!jwtToken || typeof jwtToken !== 'string') {
      return null
    }
    const tokenParts = jwtToken.split('.')
    if (tokenParts.length < 2) {
      return null
    }

    try {
      const base64 = tokenParts[1].replace(/-/g, '+').replace(/_/g, '/')
      const jsonText = decodeURIComponent(
        atob(base64)
          .split('')
          .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
          .join('')
      )
      return JSON.parse(jsonText)
    } catch (error) {
      return null
    }
  }

  const syncRoleFromToken = () => {
    const payload = decodeJwtPayload(token.value)
    roleCode.value = payload?.roleCode || ''
  }

  // 设置 token：登录/退出时更新
  const setToken = (value) => {
    token.value = value || ''
    syncRoleFromToken()
  }

  const setTokenName = (value) => {
    tokenName.value = value || 'Authorization'
  }

  const clearUserInfo = () => {
    token.value = ''
    tokenName.value = 'Authorization'
    roleCode.value = ''
  }

  const hasAuthority = (authority) => {
    if (!authority) {
      return false
    }
    return roleCode.value === authority || roleCode.value === `ROLE_${authority}`
  }

  const hasAnyAuthority = (authorities) => {
    if (!Array.isArray(authorities) || authorities.length === 0) {
      return false
    }
    return authorities.some((item) => hasAuthority(item))
  }

  const isSuperAdmin = computed(() => hasAuthority('SUPER_ADMIN'))
  const isAdmin = computed(() => hasAuthority('ADMIN') || hasAuthority('SUPER_ADMIN'))

  watch(token, () => {
    syncRoleFromToken()
  }, { immediate: true })

  return {
    token,
    tokenName,
    roleCode,
    isSuperAdmin,
    isAdmin,
    setToken,
    setTokenName,
    hasAuthority,
    hasAnyAuthority,
    clearUserInfo
  }
},
  {
    // 持久化：将 user store 存到本地存储，避免刷新后 token 丢失
    persist: true
  }
)