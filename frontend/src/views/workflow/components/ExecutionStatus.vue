<template>
  <div class="execution-status">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>执行状态</span>
          <el-button size="small" @click="refreshStatus" :loading="loading">刷新</el-button>
        </div>
      </template>
      
      <div class="status-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="执行ID">{{ executionId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(status)" size="small">
              {{ getStatusText(status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ startedAt ? formatDate(startedAt) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="执行时间">
            {{ executionTime ? `${executionTime}ms` : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 进度条（执行中时显示） -->
        <div v-if="status === 'running'" class="progress-section">
          <el-progress :percentage="progress" :status="progressStatus" />
          <div class="progress-text">执行中，请稍候...</div>
        </div>

        <!-- 错误信息 -->
        <el-alert
          v-if="status === 'failed' && errorMessage"
          type="error"
          :closable="false"
          style="margin-top: 15px;"
        >
          {{ errorMessage }}
        </el-alert>

        <!-- 输出结果 -->
        <div v-if="status === 'completed' && output" class="output-section">
          <h4>输出结果</h4>
          <pre class="output-content">{{ formattedOutput }}</pre>
        </div>

        <!-- 节点执行记录 -->
        <div v-if="nodeExecutions && nodeExecutions.length > 0" class="node-executions">
          <h4>节点执行记录</h4>
          <el-table :data="nodeExecutions" style="width: 100%">
            <el-table-column prop="node_id" label="节点ID" width="150" />
            <el-table-column prop="node_type" label="节点类型" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="execution_time" label="执行时间(ms)" width="120" />
            <el-table-column label="输出" min-width="200">
              <template #default="{ row }">
                <pre class="node-output">{{ formatOutput(row.output) }}</pre>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getExecution, type WorkflowExecution } from '@/api/workflow'
import { formatDate, getWorkflowStatusType, getWorkflowStatusText } from '@/utils/formatters'

const props = defineProps<{
  executionId: string
}>()

const loading = ref(false)
const status = ref('pending')
const startedAt = ref<string | null>(null)
const completedAt = ref<string | null>(null)
const executionTime = ref<number | null>(null)
const errorMessage = ref<string | null>(null)
const output = ref<any>(null)
const nodeExecutions = ref<any[]>([])

let pollTimer: number | null = null
let pollCount = 0
const baseInterval = 2000 // 初始轮询间隔2秒
const maxInterval = 10000 // 最大轮询间隔10秒

// 进度计算
const progress = computed(() => {
  if (status.value === 'completed') return 100
  if (status.value === 'failed') return 0
  if (status.value === 'running') {
    // 根据轮询次数估算进度（简单实现）
    return Math.min(30 + pollCount * 5, 90)
  }
  return 0
})

const progressStatus = computed(() => {
  if (status.value === 'failed') return 'exception'
  if (status.value === 'completed') return 'success'
  return null
})

// 加载执行状态
const loadStatus = async () => {
  if (!props.executionId) return
  
  loading.value = true
  try {
    const response = await getExecution(props.executionId)
    const data = response as unknown as WorkflowExecution
    
    status.value = data.status
    startedAt.value = data.started_at || null
    completedAt.value = data.completed_at || null
    executionTime.value = data.execution_time || null
    errorMessage.value = data.error_message || null
    output.value = data.output
    nodeExecutions.value = data.node_executions || []
    
    // 如果执行完成或失败，停止轮询
    if (status.value === 'completed' || status.value === 'failed') {
      stopPolling()
    }
  } catch (error: any) {
    ElMessage.error('加载执行状态失败: ' + (error.message || '未知错误'))
    console.error(error)
    // 错误时延长轮询间隔
    stopPolling()
    setTimeout(() => {
      startPolling()
    }, maxInterval * 2)
  } finally {
    loading.value = false
  }
}

// 开始轮询
const startPolling = () => {
  if (pollTimer) return
  
  pollCount = 0
  const poll = () => {
    pollCount++
    loadStatus()
    
    // 指数退避策略
    if (status.value === 'running' || status.value === 'pending') {
      const interval = Math.min(baseInterval * Math.pow(1.5, Math.floor(pollCount / 10)), maxInterval)
      pollTimer = window.setTimeout(poll, interval)
    }
  }
  
  poll()
}

// 停止轮询
const stopPolling = () => {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

// 刷新状态
const refreshStatus = () => {
  loadStatus()
}

// 格式化输出
const formatOutput = (val: any): string => {
  if (typeof val === 'object' && val !== null) {
    // 如果对象只有一个 output 字段，直接返回 output 的值
    if (Object.keys(val).length === 1 && 'output' in val) {
      return formatOutput(val.output) // 递归处理，防止 output 值也是对象
    }
    // 其他情况格式化为 JSON
    return JSON.stringify(val, null, 2)
  }
  return String(val || '')
}

// 格式化后的输出
const formattedOutput = computed(() => {
  if (!output.value) return ''
  return formatOutput(output.value)
})

// 使用公共工具函数
const getStatusType = getWorkflowStatusType
const getStatusText = getWorkflowStatusText

onMounted(() => {
  loadStatus()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.execution-status {
  margin: 20px 0;
}

/* 使用公共样式类 */

.status-content {
  margin-top: 15px;
}

.progress-section {
  margin-top: 20px;
}

.progress-text {
  margin-top: 10px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.output-section {
  margin-top: 20px;
}

.output-section h4 {
  margin-bottom: 10px;
  font-size: 16px;
}

.output-content {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 400px;
  overflow-y: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.node-executions {
  margin-top: 20px;
}

.node-executions h4 {
  margin-bottom: 10px;
  font-size: 16px;
}

.node-output {
  margin: 0;
  font-size: 12px;
  max-width: 300px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

