/**
 * 工作流执行相关的组合式函数
 */
import { ref, computed } from 'vue'
import { executeWorkflow, getExecution, type WorkflowExecution } from '@/api/workflow'
import { formatWorkflowOutput } from '@/utils/workflow'
import { showError, showSuccess, handleError } from '@/utils/message'

const baseInterval = 1000 // 初始轮询间隔1秒
const maxInterval = 10000 // 最大轮询间隔10秒

// 状态消息映射
const statusMessages: Record<string, { type: 'success' | 'error'; message: string }> = {
  completed: { type: 'success', message: '工作流执行完成' },
  failed: { type: 'error', message: '工作流执行失败' }
}

// 显示状态消息
const showStatusMessage = (status: string) => {
  const msg = statusMessages[status]
  if (msg) {
    msg.type === 'success' ? showSuccess(msg.message) : showError(msg.message)
  }
}

export function useWorkflowExecution() {
  const running = ref(false)
  const executionId = ref<string | null>(null)
  const executionStatus = ref<string>('pending')
  const executionResult = ref<WorkflowExecution | null>(null)
  const executionDialogVisible = ref(false)
  const pollTimer = ref<number | null>(null)
  const pollCount = ref(0)

  // 进度计算
  const progress = computed(() => {
    if (executionStatus.value === 'completed') return 100
    if (executionStatus.value === 'failed') return 0
    if (executionStatus.value === 'running') {
      // 根据轮询次数估算进度（简单实现）
      return Math.min(30 + pollCount.value * 5, 90)
    }
    return 0
  })

  const progressStatus = computed(() => {
    if (executionStatus.value === 'failed') return 'exception'
    if (executionStatus.value === 'completed') return 'success'
    return null
  })

  // 开始轮询
  const startPolling = () => {
    if (pollTimer.value) return
    if (!executionId.value) return

    pollCount.value = 0
    const poll = async () => {
      pollCount.value++
      
      if (!executionId.value) {
        stopPolling()
        return
      }
      try {
        const result = await getExecution(executionId.value!)
        
        executionStatus.value = result.status
        executionResult.value = result

        // 如果执行完成或失败，停止轮询
        if (result.status === 'completed' || result.status === 'failed') {
          stopPolling()
          showStatusMessage(result.status)
        } else {
          // 继续轮询，使用指数退避策略
          const interval = Math.min(
            baseInterval * Math.pow(1.5, Math.floor(pollCount.value / 10)),
            maxInterval
          )
          pollTimer.value = window.setTimeout(poll, interval)
        }
      } catch (error: any) {
        handleError(error, '轮询执行状态失败', true)
        // 错误时延长轮询间隔
        stopPolling()
        setTimeout(() => {
          if (executionId.value && (executionStatus.value === 'running' || executionStatus.value === 'pending')) {
            startPolling()
          }
        }, maxInterval * 2)
      }
    }

    poll()
  }

  // 停止轮询
  const stopPolling = () => {
    if (pollTimer.value) {
      clearTimeout(pollTimer.value)
      pollTimer.value = null
    }
  }

  // 执行工作流
  const runWorkflow = async (workflowUuid: string, request: { input?: Record<string, any>; llm_model_id?: string }) => {
    running.value = true
    executionId.value = null
    executionStatus.value = 'pending'
    executionResult.value = null
    pollCount.value = 0

    try {
      const result = await executeWorkflow(workflowUuid, request)

      const execId = result.execution_id || ''
      executionId.value = execId
      executionStatus.value = result.status
      executionResult.value = { ...result, executionId: execId } as WorkflowExecution

      // 显示执行结果对话框
      executionDialogVisible.value = true

      // 如果状态是 running 或 pending，开始轮询
      // 轮询会在状态变为 completed 或 failed 时显示消息
      if (result.status === 'running' || result.status === 'pending') {
        startPolling()
      } else {
        stopPolling()
        // 如果初始状态就是 completed 或 failed，直接显示消息
        showStatusMessage(result.status)
      }
    } catch (error: any) {
      handleError(error, '执行工作流失败', true)
      executionStatus.value = 'failed'
      const errorMsg = error?.message || '执行工作流失败'
      executionResult.value = {
        executionId: '',
        execution_id: '',
        workflow_id: workflowUuid,
        status: 'failed',
        error_message: errorMsg,
        errorMessage: errorMsg,
        execution_time: 0,
        executionTime: 0,
        node_executions: [],
        nodeExecutions: []
      } as WorkflowExecution
      executionDialogVisible.value = true
      showStatusMessage('failed')
    } finally {
      running.value = false
    }
  }

  // 重置执行状态
  const resetExecution = () => {
    stopPolling()
    running.value = false
    executionId.value = null
    executionStatus.value = 'pending'
    executionResult.value = null
    pollCount.value = 0
  }

  return {
    running,
    executionId,
    executionStatus,
    executionResult,
    executionDialogVisible,
    progress,
    progressStatus,
    runWorkflow,
    startPolling,
    stopPolling,
    resetExecution,
    formatOutput: formatWorkflowOutput
  }
}

