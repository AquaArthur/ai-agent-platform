import type { Ref } from 'vue'

/**
 * Store 异步操作包装器
 * 统一处理 loading 状态和错误处理
 * 
 * @param loading - loading 状态的 ref
 * @param operation - 要执行的异步操作
 * @param errorMessage - 错误消息前缀
 * @returns Promise<T>
 */
export const withLoading = async <T>(
  loading: Ref<boolean>,
  operation: () => Promise<T>,
  errorMessage: string
): Promise<T> => {
  loading.value = true
  try {
    return await operation()
  } catch (error) {
    console.error(errorMessage, error)
    throw error
  } finally {
    loading.value = false
  }
}

