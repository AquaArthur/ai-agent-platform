import type { Ref } from 'vue'
import { handleError } from './message'

/**
 * Store 异步操作包装器
 * 统一处理 loading 状态和错误处理
 * 
 * @param loading - loading 状态的 ref
 * @param operation - 要执行的异步操作
 * @param errorMessage - 错误消息前缀
 * @param showMessage - 是否显示错误消息（默认 false，由调用方决定是否显示）
 * @returns Promise<T>
 */
export const withLoading = async <T>(
  loading: Ref<boolean>,
  operation: () => Promise<T>,
  errorMessage: string,
  showMessage: boolean = false
): Promise<T> => {
  loading.value = true
  try {
    return await operation()
  } catch (error) {
    if (showMessage) {
      handleError(error, errorMessage, true)
    } else {
      // 不显示消息时，只在开发环境输出日志
      if (import.meta.env.DEV) {
        console.error(errorMessage, error)
      }
    }
    throw error
  } finally {
    loading.value = false
  }
}

