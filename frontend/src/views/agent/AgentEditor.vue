<template>
  <div class="agent-editor-container">
    <el-row :gutter="20" class="editor-layout">
      <!-- 表单编辑器 -->
      <el-col :span="24">
        <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button :icon="ArrowLeft" @click="handleBack">返回</el-button>
            <span class="card-title">{{ isEdit ? '编辑智能体' : '新建智能体' }}</span>
          </div>
          <div class="header-right">
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" :loading="loading" @click="handleSave">
              {{ isEdit ? '保存' : '创建' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        label-position="right"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="智能体名称" prop="name">
              <el-input
                v-model="formData.name"
                placeholder="请输入智能体名称"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio label="draft">草稿</el-radio>
                <el-radio label="published">已发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入智能体描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="系统提示词" prop="prompt">
          <MonacoEditor
            v-model="promptValue"
            language="markdown"
            height="300px"
          />
          <div class="form-tip">
            提示词用于定义智能体的角色、行为准则和回答风格
          </div>
        </el-form-item>

        <el-form-item label="提示词模板" prop="promptTemplate">
          <el-input
            v-model="formData.promptTemplate"
            type="textarea"
            :rows="4"
            placeholder="请输入提示词模板（可选）"
            maxlength="1000"
            show-word-limit
          />
          <div class="form-tip">可以使用模板变量，如 &#123;&#123;variable&#125;&#125;</div>
        </el-form-item>

        <!-- 模型配置 -->
        <el-divider content-position="left">模型配置</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择模型">
              <el-select
                v-model="modelConfigForm.modelId"
                placeholder="请选择模型"
                clearable
                filterable
                style="width: 100%"
                :loading="loadingModels"
                @visible-change="loadModelsIfNeeded"
              >
                <el-option
                  v-for="model in llmModels"
                  :key="model.id"
                  :label="getModelLabel(model)"
                  :value="model.id"
                  :disabled="!model.isActive"
                >
                  <div class="model-option">
                    <div class="model-option-name">{{ model.displayName || model.name }}</div>
                    <div class="model-option-info">
                      <el-tag v-if="model.provider" size="small" type="info">{{ model.provider }}</el-tag>
                      <el-tag v-if="model.isDefault" size="small" type="success">默认</el-tag>
                      <span v-if="model.description" class="model-desc">{{ model.description }}</span>
                    </div>
                  </div>
                </el-option>
              </el-select>
              <div class="form-tip">选择智能体使用的语言模型</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="温度">
              <el-slider
                v-model="modelConfigForm.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                show-input
                :show-input-controls="false"
              />
              <div class="form-tip">控制输出的随机性，范围 0-2</div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 关联配置 -->
        <el-divider content-position="left">关联配置</el-divider>

        <el-form-item label="绑定知识库">
          <el-select
            v-model="selectedKnowledgeBaseIds"
            multiple
            placeholder="请选择知识库（可选）"
            clearable
            filterable
            style="width: 100%"
            :loading="loadingKnowledgeBases"
            @visible-change="loadKnowledgeBasesIfNeeded"
            @change="handleKnowledgeBasesChange"
          >
            <el-option
              v-for="kb in availableKnowledgeBases"
              :key="kb.uuid"
              :label="kb.name"
              :value="kb.id || kb.uuid"
            >
              <div class="kb-option">
                <div class="kb-option-name">
                  {{ kb.name }}
                  <el-tag v-if="kb.scopeType" size="small" :type="getScopeTagType(kb.scopeType)">
                    {{ getScopeLabel(kb.scopeType) }}
                  </el-tag>
                  <el-tag v-if="kb.accessLevel" size="small" :type="getAccessLevelTagType(kb.accessLevel)">
                    {{ getAccessLevelLabel(kb.accessLevel) }}
                  </el-tag>
                </div>
                <div v-if="kb.description" class="kb-option-desc">{{ kb.description }}</div>
                <div class="kb-option-info">
                  <span class="info-text">文档数: {{ kb.documentCount || 0 }}</span>
                  <span class="info-text">块数: {{ kb.chunkCount || 0 }}</span>
                </div>
              </div>
            </el-option>
          </el-select>
          <div class="form-tip">为智能体选择可用的知识库</div>
          <div v-if="selectedKnowledgeBaseIds.length > 0" class="selected-kbs">
            <div class="selected-count">已选择 {{ selectedKnowledgeBaseIds.length }} 个知识库</div>
          </div>
        </el-form-item>

        <el-form-item label="绑定插件">
          <el-select
            v-model="selectedPluginIdForAdd"
            placeholder="选择要添加的插件"
            filterable
            style="width: 100%"
            :loading="loadingPlugins"
            @visible-change="loadPluginsIfNeeded"
            @change="handleAddPlugin"
          >
            <el-option
              v-for="plugin in unselectedPlugins"
              :key="plugin.id"
              :label="plugin.name"
              :value="plugin.id!"
              :disabled="!plugin.isEnabled && plugin.status !== 'enabled'"
            >
              <div class="plugin-option">
                <div class="plugin-option-name">
                  {{ plugin.name }}
                  <el-tag v-if="plugin.status === 'enabled' || plugin.isEnabled" size="small" type="success">已启用</el-tag>
                  <el-tag v-else size="small" type="info">已禁用</el-tag>
                  <el-tag v-if="!plugin.userId" size="small" type="warning">系统</el-tag>
                </div>
                <div v-if="plugin.description" class="plugin-option-desc">{{ plugin.description }}</div>
              </div>
            </el-option>
          </el-select>
          <div class="form-tip">选择插件后可配置优先级和启用状态</div>
        </el-form-item>

        <!-- 已配置的插件列表 -->
        <el-form-item v-if="pluginConfigs.length > 0" label="插件配置">
          <div class="plugin-config-list">
            <div
              v-for="(config, index) in sortedPluginConfigs"
              :key="config.pluginId"
              class="plugin-config-item"
            >
              <div class="plugin-config-main">
                <el-switch
                  v-model="config.enabled"
                  :active-text="''"
                  :inactive-text="''"
                  size="small"
                  @change="handlePluginConfigChange"
                />
                <div class="plugin-config-info">
                  <span class="plugin-config-name">{{ getPluginName(config.pluginId) }}</span>
                  <el-tag v-if="!config.enabled" size="small" type="info">已禁用</el-tag>
                </div>
              </div>
              <div class="plugin-config-actions">
                <el-input-number
                  v-model="config.priority"
                  :min="0"
                  :max="100"
                  size="small"
                  controls-position="right"
                  placeholder="优先级"
                  style="width: 100px"
                  @change="handlePluginConfigChange"
                />
                <el-button
                  type="danger"
                  size="small"
                  :icon="Delete"
                  circle
                  @click="handleRemovePlugin(index)"
                />
              </div>
            </div>
          </div>
          <div class="form-tip">优先级数值越大越先执行，可通过开关临时禁用插件</div>
        </el-form-item>
      </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Delete } from '@element-plus/icons-vue'
import { useAgentStore } from '@/stores/useAgentStore'
import { getLlmModelList } from '@/api/llm'
import { getPluginList } from '@/api'
import { getKnowledgeBaseList, type KnowledgeBase } from '@/api/knowledgeBase'
import { getAgentKnowledgeBases, syncAgentKnowledgeBases } from '@/api/agent'
import { getScopeTagType, getScopeLabel, getAccessLevelTagType, getAccessLevelLabel } from '@/utils/formatters'
import type { Agent, LlmModel, Plugin, AgentPluginConfig } from '@/types/entity'
import MonacoEditor from '@/components/MonacoEditor.vue'

const route = useRoute()
const router = useRouter()
const agentStore = useAgentStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

// 是否为编辑模式
const agentId = computed(() => route.params.id as string)
const isEdit = computed(() => agentId.value && agentId.value !== 'new')

// 表单数据
const formData = ref<Agent>({
  name: '',
  description: '',
  prompt: '',
  promptTemplate: '',
  status: 'draft',
  knowledgeBaseId: '',
  kbIds: [],
  toolsConfig: []
})

// 模型配置表单（从 modelConfig 中提取）
const modelConfigForm = ref({
  modelId: '',
  temperature: 0.7
})

// 选中的知识库ID列表（用于多选下拉框）
const selectedKnowledgeBaseIds = ref<string[]>([])
// 插件配置列表（包含优先级和启用状态）
const pluginConfigs = ref<AgentPluginConfig[]>([])

// LLM模型列表
const llmModels = ref<LlmModel[]>([])
const loadingModels = ref(false)
const modelsLoaded = ref(false)

// 插件列表
const availablePlugins = ref<Plugin[]>([])
const loadingPlugins = ref(false)
const pluginsLoaded = ref(false)
// 用于添加新插件的临时选择
const selectedPluginIdForAdd = ref<string>('')

// 未选择的插件列表（排除已配置的）
const unselectedPlugins = computed(() => {
  const configuredIds = pluginConfigs.value.map(c => c.pluginId)
  return availablePlugins.value.filter(p => !configuredIds.includes(p.id!))
})

// 按优先级排序的插件配置
const sortedPluginConfigs = computed(() => {
  return [...pluginConfigs.value].sort((a, b) => b.priority - a.priority)
})

// 获取插件名称
const getPluginName = (pluginId: string): string => {
  const plugin = availablePlugins.value.find(p => p.id === pluginId)
  return plugin?.name || pluginId
}

// 添加插件到配置
const handleAddPlugin = (pluginId: string) => {
  if (!pluginId) return
  
  // 检查是否已存在
  if (pluginConfigs.value.some(c => c.pluginId === pluginId)) {
    return
  }
  
  // 计算新优先级（比当前最高优先级高1）
  const maxPriority = pluginConfigs.value.length > 0 
    ? Math.max(...pluginConfigs.value.map(c => c.priority)) 
    : 0
  
  pluginConfigs.value.push({
    pluginId,
    priority: maxPriority + 1,
    enabled: true
  })
  
  // 清空选择
  selectedPluginIdForAdd.value = ''
  
  // 同步到 formData
  syncPluginConfigsToForm()
}

// 移除插件配置
const handleRemovePlugin = (index: number) => {
  // 在排序后的列表中找到真正的索引
  const sortedConfig = sortedPluginConfigs.value[index]
  if (!sortedConfig) return
  const realIndex = pluginConfigs.value.findIndex(c => c.pluginId === sortedConfig.pluginId)
  if (realIndex !== -1) {
    pluginConfigs.value.splice(realIndex, 1)
    syncPluginConfigsToForm()
  }
}

// 处理插件配置变更
const handlePluginConfigChange = () => {
  syncPluginConfigsToForm()
}

// 同步插件配置到表单数据
const syncPluginConfigsToForm = () => {
  formData.value.toolsConfig = pluginConfigs.value.map(c => ({
    pluginId: c.pluginId,
    priority: c.priority,
    enabled: c.enabled
  }))
}

// 知识库列表
const availableKnowledgeBases = ref<KnowledgeBase[]>([])
const loadingKnowledgeBases = ref(false)
const knowledgeBasesLoaded = ref(false)

// 表单验证规则
const formRules: FormRules = {
  name: [
    { required: true, message: '请输入智能体名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '描述不能超过 500 个字符', trigger: 'blur' }
  ],
  prompt: [
    { max: 2000, message: '提示词不能超过 2000 个字符', trigger: 'blur' }
  ]
}

// prompt 的计算属性，确保始终是字符串
const promptValue = computed({
  get: () => formData.value.prompt || '',
  set: (value: string) => {
    formData.value.prompt = value
  }
})

// 处理知识库ID列表变化
const handleKnowledgeBasesChange = (value: string[]) => {
  formData.value.kbIds = value || []
  // 如果只选择了一个知识库，也设置 knowledgeBaseId（兼容旧字段）
  if (value && value.length === 1) {
    formData.value.knowledgeBaseId = value[0]
  } else {
    formData.value.knowledgeBaseId = ''
  }
}


// 获取模型标签
const getModelLabel = (model: LlmModel) => {
  return model.displayName || model.name
}

// 加载模型列表
const loadModels = async () => {
  if (modelsLoaded.value) return
  
  loadingModels.value = true
  try {
    llmModels.value = await getLlmModelList()
    // 只显示激活的模型
    llmModels.value = llmModels.value.filter((model) => model.isActive !== false)
    modelsLoaded.value = true
  } catch (error: any) {
    console.error('加载模型列表失败:', error)
    ElMessage.error(error.message || '加载模型列表失败')
  } finally {
    loadingModels.value = false
  }
}

// 需要时加载模型列表
const loadModelsIfNeeded = (visible: boolean) => {
  if (visible && !modelsLoaded.value) {
    loadModels()
  }
}

// 加载插件列表
const loadPlugins = async () => {
  if (pluginsLoaded.value) return
  
  loadingPlugins.value = true
  try {
    const result = await getPluginList()
    availablePlugins.value = result.list || []
    pluginsLoaded.value = true
  } catch (error: any) {
    console.error('加载插件列表失败:', error)
    ElMessage.error(error.message || '加载插件列表失败')
  } finally {
    loadingPlugins.value = false
  }
}

// 需要时加载插件列表
const loadPluginsIfNeeded = (visible: boolean) => {
  if (visible && !pluginsLoaded.value) {
    loadPlugins()
  }
}

// 加载知识库列表
const loadKnowledgeBases = async () => {
  if (knowledgeBasesLoaded.value) return
  
  loadingKnowledgeBases.value = true
  try {
    const result = await getKnowledgeBaseList({ pageSize: 1000 }) // 获取所有知识库
    availableKnowledgeBases.value = result.list || []
    knowledgeBasesLoaded.value = true
  } catch (error: any) {
    console.error('加载知识库列表失败:', error)
    ElMessage.error(error.message || '加载知识库列表失败')
  } finally {
    loadingKnowledgeBases.value = false
  }
}

// 需要时加载知识库列表
const loadKnowledgeBasesIfNeeded = (visible: boolean) => {
  if (visible && !knowledgeBasesLoaded.value) {
    loadKnowledgeBases()
  }
}

// 初始化表单数据
const initFormData = async () => {
  if (isEdit.value) {
    // 编辑模式：加载现有数据
    try {
      await agentStore.fetchAgentById(agentId.value)
      const agent = agentStore.currentAgent
      if (agent) {
        // 处理 snake_case 和 camelCase 兼容性（后端可能返回 snake_case 格式）
        const agentData = agent as Record<string, any>
        const modelCfg = agent.modelConfig || agentData.model_config
        const promptTpl = agent.promptTemplate || agentData.prompt_template || ''
        const kbId = agent.knowledgeBaseId || agentData.knowledge_base_id
        const kbIdList = agent.kbIds || agentData.kb_ids || []
        const toolsCfg = agent.toolsConfig || agentData.tools_config || []

        formData.value = {
          ...agent,
          status: agent.status || 'draft',
          prompt: agent.prompt || '',
          promptTemplate: promptTpl
        }

        // 处理模型配置
        if (modelCfg) {
          modelConfigForm.value.modelId = modelCfg.modelId || modelCfg.model_id || modelCfg.model || ''
          modelConfigForm.value.temperature = modelCfg.temperature ?? 0.7
        }

        // 通过关联API加载智能体绑定的知识库
        try {
          const linkedKbs = await getAgentKnowledgeBases(agentId.value)
          selectedKnowledgeBaseIds.value = linkedKbs.map(kb => kb.id || kb.uuid)
        } catch (kbError) {
          console.warn('加载智能体关联知识库失败，尝试使用旧字段:', kbError)
          // 降级处理：使用旧字段
          if (kbIdList.length > 0) {
            selectedKnowledgeBaseIds.value = kbIdList
          } else if (kbId) {
            selectedKnowledgeBaseIds.value = [kbId]
          } else {
            selectedKnowledgeBaseIds.value = []
          }
        }

        // 处理插件配置（兼容旧格式：string[] 和新格式：AgentPluginConfig[]）
        if (toolsCfg && toolsCfg.length > 0) {
          // 检查是否是新格式（对象数组）
          if (typeof toolsCfg[0] === 'object' && toolsCfg[0] !== null) {
            pluginConfigs.value = toolsCfg.map((c: any, index: number) => ({
              pluginId: c.pluginId || c.plugin_id || '',
              priority: c.priority ?? index,
              enabled: c.enabled ?? true
            }))
          } else {
            // 旧格式：string[] -> 转换为新格式
            pluginConfigs.value = (toolsCfg as string[]).map((id: string, index: number) => ({
              pluginId: id,
              priority: index,
              enabled: true
            }))
          }
        } else {
          pluginConfigs.value = []
        }
      }
    } catch (error: any) {
      ElMessage.error(error.message || '加载智能体信息失败')
      router.push({ name: 'agent-list' })
    }
  } else {
    // 新建模式：重置表单
    formData.value = {
      name: '',
      description: '',
      prompt: '',
      promptTemplate: '',
      status: 'draft',
      knowledgeBaseId: '',
      kbIds: [],
      toolsConfig: []
    }
    modelConfigForm.value = {
      modelId: '',
      temperature: 0.7
    }
    selectedKnowledgeBaseIds.value = []
    pluginConfigs.value = []
  }
}

// 保存表单
const handleSave = async () => {
  if (!formRef.value) return

  try {
    // 验证表单
    await formRef.value.validate()

    // 构建模型配置
    const modelConfig: Record<string, any> = {}
    if (modelConfigForm.value.modelId) {
      modelConfig.modelId = modelConfigForm.value.modelId
      modelConfig.model = modelConfigForm.value.modelId
    }
    if (modelConfigForm.value.temperature !== undefined) {
      modelConfig.temperature = modelConfigForm.value.temperature
    }

    // 同步插件配置到表单
    syncPluginConfigsToForm()

    // 构建提交数据（不再包含知识库字段，由关联API管理）
    const submitData: Agent = {
      ...formData.value,
      modelConfig: Object.keys(modelConfig).length > 0 ? modelConfig : undefined,
      // 清空旧的知识库字段，使用关联表管理
      kbIds: [],
      knowledgeBaseId: '',
      // 使用最新的插件配置
      toolsConfig: pluginConfigs.value.map(c => ({
        pluginId: c.pluginId,
        priority: c.priority,
        enabled: c.enabled
      }))
    }

    loading.value = true

    let savedAgentId: string

    if (isEdit.value) {
      // 更新智能体基本信息
      await agentStore.editAgent(agentId.value, submitData)
      savedAgentId = agentId.value
    } else {
      // 创建智能体
      const created = await agentStore.addAgent(submitData)
      savedAgentId = created.id!
    }

    // 同步知识库关联（使用关联API）
    const targetKbIds = selectedKnowledgeBaseIds.value || []
    try {
      await syncAgentKnowledgeBases(savedAgentId, targetKbIds)
    } catch (syncError: any) {
      console.error('同步知识库关联失败:', syncError)
      ElMessage.warning('知识库关联同步失败，请在编辑页面重试')
    }

    ElMessage.success(isEdit.value ? '保存成功' : '创建成功')

    if (isEdit.value) {
      // 刷新数据
      await initFormData()
    } else {
      // 跳转到编辑页面
      router.replace({ name: 'agent-editor', params: { id: savedAgentId } })
    }
  } catch (error: any) {
    console.error('保存失败:', error)
    if (error !== false) {
      // error !== false 表示不是表单验证失败
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    loading.value = false
  }
}

// 取消编辑
const handleCancel = () => {
  router.push({ name: 'agent-list' })
}

// 返回列表
const handleBack = () => {
  router.push({ name: 'agent-list' })
}

// 监听路由变化，重新加载数据
watch(
  () => route.params.id,
  () => {
    initFormData()
  },
  { immediate: false }
)

// 初始化
onMounted(() => {
  initFormData()
  // 预加载模型和插件列表
  loadModels()
  loadPlugins()
  // 预加载知识库列表
  loadKnowledgeBases()
})
</script>

<style scoped>
/* 使用公共布局样式 */
.agent-editor-container {
  padding: 20px;
  height: calc(100vh - 40px);
  overflow: hidden;
  background: var(--gradient-bg-primary);
  display: flex;
  flex-direction: column;
}

.editor-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.el-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
}

:deep(.el-card__body::-webkit-scrollbar) {
  width: 8px;
}

:deep(.el-card__body::-webkit-scrollbar-track) {
  background: #f1f3f5;
  border-radius: 4px;
}

:deep(.el-card__body::-webkit-scrollbar-thumb) {
  background: #cbd5e1;
  border-radius: 4px;
  transition: background 0.3s ease;
}

:deep(.el-card__body::-webkit-scrollbar-thumb:hover) {
  background: #94a3b8;
}

/* 使用公共样式类 */

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 使用公共样式类 */

:deep(.el-divider__text) {
  font-size: 14px;
  font-weight: 600;
}

/* 模型选项样式 */
.model-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.model-option-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.model-option-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.model-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

/* 插件选项样式 */
.plugin-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.plugin-option-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.plugin-option-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.plugin-option-id {
  display: flex;
  align-items: center;
}

.identifier-text {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  font-family: monospace;
}

/* 已选择插件信息 */
.selected-plugins {
  margin-top: 8px;
}

.selected-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 知识库选项样式 */
.kb-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kb-option-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.kb-option-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.kb-option-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-text {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

/* 已选择知识库信息 */
.selected-kbs {
  margin-top: 8px;
}

/* 插件配置列表样式 */
.plugin-config-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
}

.plugin-config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 12px;
  transition: all 0.2s ease;
}

.plugin-config-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.plugin-config-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.plugin-config-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.plugin-config-name {
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-config-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.plugin-config-actions :deep(.el-input-number) {
  --el-input-number-unit-width: 18px;
}

.plugin-config-actions :deep(.el-input-number .el-input__inner) {
  text-align: center;
}
</style>

