<template>
  <div class="execution-panel">
    <div class="panel-header">
      <h3 class="panel-title">运行工作流</h3>
      <el-button circle text @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div class="panel-content">
      <!-- 输入参数区域 -->
      <div class="section-card">
        <div class="section-header">
          <el-icon><Edit /></el-icon>
          <span>输入变量</span>
        </div>
        
        <div class="params-form">
          <template v-if="startNodeParams.length > 0">
            <el-form :model="runParams" label-position="top">
              <el-form-item
                v-for="param in startNodeParams"
                :key="param.name"
                :label="param.name + (param.description ? ` (${param.description})` : '')"
                :required="param.required"
              >
                <el-input
                  v-if="param.type === 'string'"
                  v-model="runParams[param.name]"
                  :placeholder="'请输入 ' + param.name"
                  type="textarea"
                  :rows="2"
                />
                <el-input-number
                  v-else-if="param.type === 'number'"
                  v-model="runParams[param.name]"
                  style="width: 100%"
                />
                <el-switch
                  v-else-if="param.type === 'boolean'"
                  v-model="runParams[param.name]"
                />
                <el-input
                  v-else
                  v-model="runParams[param.name]"
                />
              </el-form-item>
            </el-form>
          </template>
          <div v-else class="empty-state">
            <span>无需输入参数</span>
          </div>
          
          <el-button 
            type="primary" 
            class="run-btn" 
            :loading="running" 
            @click="handleRun"
            :icon="VideoPlay"
          >
            {{ running ? '执行中...' : '开始运行' }}
          </el-button>
        </div>
      </div>

      <!-- 执行状态区域 -->
      <div v-if="executionId" class="result-area">
        <el-divider content-position="center">执行状态</el-divider>
        <ExecutionStatus :execution-id="executionId" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { VideoPlay, Close, Edit } from '@element-plus/icons-vue'
import ExecutionStatus from './ExecutionStatus.vue'

const props = defineProps<{
  nodes: any[]
  startNodeParams: any[]
  running: boolean
  executionId?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'run', params: Record<string, any>): void
}>()

const runParams = ref<Record<string, any>>({})

// 监听参数定义变化，初始化输入值
watch(() => props.startNodeParams, (params) => {
  const newParams: Record<string, any> = {}
  params.forEach((p: any) => {
    if (p.type === 'boolean') {
      newParams[p.name] = false
    } else {
      newParams[p.name] = ''
    }
  })
  runParams.value = newParams
}, { immediate: true })

const handleRun = () => {
  emit('run', runParams.value)
}
</script>

<style scoped>
.execution-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  width: 100%;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.section-card {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.empty-state {
  padding: 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 16px;
}

.run-btn {
  width: 100%;
  margin-top: 8px;
}

.result-area {
  margin-top: 20px;
}

/* 调整表单间距 */
:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>

