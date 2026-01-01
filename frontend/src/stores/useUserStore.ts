import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/user'
import { showSuccess, showError } from '@/utils/message'

/**
 * 用户信息接口
 */
export interface UserInfo {
  userId?: string
  username?: string
  nickname?: string
  displayName?: string
  avatar?: string
  role?: string      // 后端返回的单个角色
  roles?: string[]   // 前端使用的角色列表（从 role 转换）
}

/**
 * 默认用户信息
 */
const DEFAULT_USER_INFO: UserInfo = {
  username: '游客',
  displayName: '游客',
  roles: []
}

/**
 * 用户 Store
 * 管理用户信息和认证状态
 */
export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo>({ ...DEFAULT_USER_INFO })
  const token = ref<string>(localStorage.getItem('token') || '')

  /**
   * 判断用户是否是管理员
   */
  const isAdmin = computed(() => {
    const roles = userInfo.value.roles || []
    const role = userInfo.value.role || ''
    return (
      roles.some(r =>
        r.toLowerCase() === 'admin' ||
        r.toLowerCase() === 'administrator' ||
        r.toLowerCase() === '管理员'
      ) || role.toLowerCase() === 'admin'
    )
  })

  /**
   * 判断用户是否已登录
   */
  const isLoggedIn = computed(() => {
    return !!token.value
  })

  const persistAuthState = () => {
    localStorage.setItem('token', token.value || '')
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value || {}))
  }

  /**
   * 用户登录
   */
  const login = async (username: string, password: string): Promise<void> => {
    try {
      const response = await loginApi({ username, password })
      
      if (!response?.token) {
        throw new Error('登录失败：未返回有效凭证')
      }
      
      // 保存认证信息
      token.value = response.token
      userInfo.value = {
        userId: response.userId,
        username: response.username,
        nickname: response.nickname,
        displayName: response.nickname || response.username,
        role: response.role,
        roles: [response.role]
      }
      
      persistAuthState()
      showSuccess('登录成功')
    } catch (error: any) {
      // 清除认证信息
      token.value = ''
      userInfo.value = { ...DEFAULT_USER_INFO }
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      
      showError(error.message || '登录失败')
      throw error
    }
  }

  /**
   * 用户注册
   */
  const register = async (username: string, password: string, nickname?: string): Promise<void> => {
    try {
      const response = await registerApi({ username, password, nickname })
      
      if (!response?.token) {
        throw new Error('注册失败：未返回有效凭证')
      }
      
      // 保存认证信息
      token.value = response.token
      userInfo.value = {
        userId: response.userId,
        username: response.username,
        nickname: response.nickname,
        displayName: response.nickname || response.username,
        role: response.role,
        roles: [response.role]
      }
      
      persistAuthState()
      showSuccess('注册成功')
    } catch (error: any) {
      // 清除认证信息
      token.value = ''
      userInfo.value = { ...DEFAULT_USER_INFO }
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      
      showError(error.message || '注册失败')
      throw error
    }
  }

  /**
   * 用户登出
   */
  const logout = () => {
    token.value = ''
    userInfo.value = { ...DEFAULT_USER_INFO }
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    showSuccess('已退出登录')
    window.location.href = '/login'
  }

  /**
   * 恢复用户状态
   */
  const restoreUserState = () => {
    const savedToken = localStorage.getItem('token')
    const savedUserInfo = localStorage.getItem('userInfo')

    if (savedToken) {
      token.value = savedToken
    }

    if (savedUserInfo) {
      try {
        userInfo.value = { ...DEFAULT_USER_INFO, ...JSON.parse(savedUserInfo) }
      } catch {
        // 解析失败，使用默认值
      }
    }
  }

  // 初始化时恢复用户状态
  restoreUserState()

  return {
    userInfo,
    token,
    isAdmin,
    isLoggedIn,
    login,
    register,
    logout,
    restoreUserState
  }
})
