import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 用户信息接口
 */
export interface UserInfo {
  username?: string
  displayName?: string
  avatar?: string
  roles?: string[]
}

/**
 * 默认用户信息
 */
const DEFAULT_USER_INFO: UserInfo = {
  username: '管理员',
  displayName: '管理员',
  roles: []
}

/**
 * 用户 Store
 * 管理用户信息和认证状态
 */
export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo>({ ...DEFAULT_USER_INFO })

  /**
   * 登出用户
   * 重置用户信息，可在此处添加清除 token 等逻辑
   */
  const logout = () => {
    userInfo.value = { ...DEFAULT_USER_INFO }
  }

  return {
    userInfo,
    logout
  }
})

