/**
 * 统一的消息提示工具
 * 封装 Element Plus 的 ElMessage，提供统一的错误处理接口
 */
import { ElMessage } from 'element-plus'

/**
 * 显示错误消息
 * @param error - 错误对象或错误消息字符串
 * @param defaultMessage - 默认错误消息（当 error 为空时使用）
 */
export const showError = (error: any, defaultMessage: string = '操作失败'): void => {
  const message = error?.message || error || defaultMessage
  ElMessage.error(message)
}

/**
 * 显示成功消息
 */
export const showSuccess = (message: string): void => {
  ElMessage.success(message)
}

/**
 * 显示警告消息
 */
export const showWarning = (message: string): void => {
  ElMessage.warning(message)
}

/**
 * 显示信息消息
 */
export const showInfo = (message: string): void => {
  ElMessage.info(message)
}

/**
 * 统一错误处理函数
 * 用于 try-catch 块中，自动提取错误消息并显示
 * @param error - 错误对象
 * @param defaultMessage - 默认错误消息
 * @param logError - 是否在控制台输出错误（开发环境默认 true）
 */
export const handleError = (
  error: any,
  defaultMessage: string = '操作失败',
  logError: boolean = import.meta.env.DEV
): void => {
  if (logError) {
    console.error(defaultMessage, error)
  }
  showError(error, defaultMessage)
}

