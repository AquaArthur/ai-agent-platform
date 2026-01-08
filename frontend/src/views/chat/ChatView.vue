<template>
  <div class="chat-view-container">
    <!-- 返回按钮 -->
    <div class="chat-header">
      <el-button :icon="ArrowLeft" @click="handleBack">返回</el-button>
      <h2 class="page-title">对话页面</h2>
    </div>
    
    <el-row :gutter="20" class="chat-layout">
      <!-- 左侧：智能体选择 -->
      <el-col :span="6">
        <el-card class="agent-select-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">选择智能体</span>
            </div>
          </template>

          <el-select
            v-model="selectedAgentId"
            placeholder="请选择智能体"
            filterable
            style="width: 100%"
            :loading="loadingAgents"
            @change="handleAgentChange"
          >
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            >
              <div class="agent-option">
                <div class="agent-name">{{ agent.name }}</div>
                <div v-if="agent.description" class="agent-desc">{{ agent.description }}</div>
              </div>
            </el-option>
          </el-select>

          <div v-if="currentAgent" class="agent-info">
            <el-divider />
            <div class="info-item">
              <span class="info-label">状态：</span>
              <el-tag :type="currentAgent.status === 'published' ? 'success' : 'info'">
                {{ currentAgent.status === 'published' ? '已发布' : '草稿' }}
              </el-tag>
            </div>
            <div v-if="currentAgent.description" class="info-item">
              <span class="info-label">描述：</span>
              <span class="info-value">{{ currentAgent.description }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">模型：</span>
              <span class="info-value">{{ getModelId() || '未配置' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：对话区域 -->
      <el-col :span="18">
        <ChatPanel
          :agent-id="selectedAgentId || undefined"
          :llm-model-id="routeModelId || getModelId() || undefined"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getAgentList } from '@/api/agent'
import type { Agent } from '@/types/entity'
import ChatPanel from '@/views/agent/components/ChatPanel.vue'

const route = useRoute()
const router = useRouter()

// 从路由参数获取模型ID
const routeModelId = computed(() => route.query.modelId as string | undefined)

// 智能体列表
const agents = ref<Agent[]>([])
const loadingAgents = ref(false)
const selectedAgentId = ref<string>('')

// 当前选中的智能体
const currentAgent = computed(() => {
  return agents.value.find((agent) => agent.id === selectedAgentId.value)
})

// 获取模型ID
const getModelId = (): string | undefined => {
  if (!currentAgent.value || !currentAgent.value.modelConfig) {
    return undefined
  }
  return (
    currentAgent.value.modelConfig.modelId ||
    currentAgent.value.modelConfig.model
  )
}

// 加载智能体列表
const loadAgents = async () => {
  loadingAgents.value = true
  try {
    agents.value = await getAgentList()
  } catch (error: any) {
    console.error('加载智能体列表失败:', error)
    ElMessage.error(error.message || '加载智能体列表失败')
  } finally {
    loadingAgents.value = false
  }
}

// 处理智能体切换
const handleAgentChange = (agentId: string) => {
  if (agentId && !getModelId()) {
    ElMessage.warning('该智能体未配置模型，无法进行对话')
  }
}

// 返回按钮处理
const handleBack = () => {
  // 如果是从模型管理页面进入的，返回模型管理页面
  if (routeModelId.value) {
    router.push('/main/models')
  } else {
    // 否则返回上一页或智能体管理页面
    router.back()
  }
}

// 初始化
onMounted(async () => {
  await loadAgents()
  
  // 如果路由中有 modelId 参数，尝试找到使用该模型的智能体
  if (routeModelId.value) {
    const agentWithModel = agents.value.find(agent => {
      const modelId = agent.modelConfig?.modelId || agent.modelConfig?.model
      return modelId === routeModelId.value
    })
    if (agentWithModel?.id) {
      selectedAgentId.value = agentWithModel.id
      handleAgentChange(agentWithModel.id)
    } else {
      ElMessage.info('未找到使用该模型的智能体，请手动选择智能体')
    }
  } else {
    // 如果路由中有 agentId 参数，则自动选择该智能体
    const agentIdFromQuery = route.query.agentId as string
    if (agentIdFromQuery && agents.value.some(agent => agent.id === agentIdFromQuery)) {
      selectedAgentId.value = agentIdFromQuery
      handleAgentChange(agentIdFromQuery)
    }
  }
})
</script>

<style scoped>
/* 使用公共布局样式 */
.chat-view-container {
  padding: 16px;
  height: calc(100vh - 32px);
  overflow: hidden;
  background: var(--gradient-bg-primary);
  min-height: calc(100vh - 32px);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.agent-select-card {
  height: 100%;
  border-radius: 12px;
}

.agent-select-card :deep(.el-card__body) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.chat-layout {
  height: calc(100% - 80px);
  margin-bottom: 0;
  overflow: hidden;
}

/* 确保右侧对话区域高度正确 */
.chat-layout :deep(.el-col) {
  height: 100%;
  overflow: hidden;
}

/* 确保 ChatPanel 组件能正确获取高度 */
.chat-layout :deep(.el-col > *) {
  height: 100%;
  max-height: 100%;
}

/* 使用公共样式类 */

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.agent-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.agent-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.agent-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-info {
  margin-top: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 8px;
}

.info-label {
  font-weight: 500;
  color: var(--el-text-color-primary);
  min-width: 50px;
}

.info-value {
  color: var(--el-text-color-secondary);
  flex: 1;
  word-break: break-word;
}
</style>

