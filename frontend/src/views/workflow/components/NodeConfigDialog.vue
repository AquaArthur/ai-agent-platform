<template>
  <el-drawer
    v-model="visible"
    :title="dialogTitle"
    size="500px"
    direction="rtl"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="config-content">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
      >
      <!-- 开始节点：无配置项 -->
      <template v-if="nodeType === 'start'">
        <el-alert
          type="info"
          :closable="false"
          show-icon
        >
          <template #title>
            <span>开始节点无需配置，它将接收工作流的输入参数。</span>
          </template>
        </el-alert>
      </template>

      <!-- 结束节点：无配置项 -->
      <template v-else-if="nodeType === 'end'">
        <el-alert
          type="info"
          :closable="false"
          show-icon
        >
          <template #title>
            <span>结束节点无需配置，它将输出工作流的执行结果。</span>
          </template>
        </el-alert>
      </template>

      <!-- LLM节点配置 -->
      <template v-else-if="nodeType === 'llm'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="智能体UUID" prop="agentUuid">
          <el-select
            v-model="formData.agentUuid"
            placeholder="请选择智能体"
            filterable
            clearable
            style="width: 100%"
            :loading="loadingAgents"
            @visible-change="loadAgentsIfNeeded"
          >
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            />
          </el-select>
          <div class="form-item-tip">选择要使用的智能体</div>
        </el-form-item>

        <el-form-item label="提示词" prop="prompt">
          <el-input
            v-model="formData.prompt"
            type="textarea"
            :rows="4"
            placeholder="请输入提示词，支持变量替换，如：{input.query}、{node_id}、{node_id.field}"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-form-item label="温度参数" prop="temperature">
          <el-slider
            v-model="formData.temperature"
            :min="0"
            :max="2"
            :step="0.1"
            show-input
            :show-input-controls="false"
            style="width: 100%"
          />
          <div class="form-item-tip">范围：0-2，默认0.7</div>
        </el-form-item>

        <el-form-item label="最大Token数" prop="maxTokens">
          <el-input-number
            v-model="formData.maxTokens"
            :min="1"
            :max="10000"
            style="width: 100%"
          />
          <div class="form-item-tip">默认2000</div>
        </el-form-item>
      </template>

      <!-- HTTP请求节点配置 -->
      <template v-else-if="nodeType === 'http'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="请求URL" prop="url">
          <el-input
            v-model="formData.url"
            placeholder="请输入请求URL，支持变量替换"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-divider content-position="left">请求配置</el-divider>

        <el-form-item label="请求方法" prop="method">
          <el-radio-group v-model="formData.method">
            <el-radio label="GET">GET</el-radio>
            <el-radio label="POST">POST</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="请求头" prop="headers">
          <div class="key-value-editor">
            <div
              v-for="(value, key, index) in formData.headers"
              :key="index"
              class="key-value-item"
            >
              <el-input
                v-model="headerKeys[index]"
                placeholder="Header名称"
                style="width: 40%"
                @input="updateHeaderKey(index, $event)"
              />
              <el-input
                v-model="formData.headers[key]"
                placeholder="Header值，支持变量替换"
                style="width: 60%"
              />
              <el-button
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="removeHeader(key)"
              />
            </div>
            <el-button
              type="primary"
              :icon="Plus"
              size="small"
              @click="addHeader"
            >
              添加请求头
            </el-button>
          </div>
        </el-form-item>

        <el-form-item
          v-if="formData.method === 'POST'"
          label="请求体"
          prop="body"
        >
          <el-input
            v-model="bodyString"
            type="textarea"
            :rows="6"
            placeholder="请输入请求体（JSON格式），支持变量替换"
            @blur="handleBodyChange"
          />
          <div class="form-item-tip">
            支持JSON格式，支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>
      </template>

      <!-- 知识库检索节点配置 -->
      <template v-else-if="nodeType === 'knowledge'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="知识库" prop="knowledgeBaseId">
          <el-select
            v-model="formData.knowledgeBaseId"
            placeholder="请选择知识库"
            filterable
            clearable
            style="width: 100%"
            :loading="loadingKnowledgeBases"
            @visible-change="loadKnowledgeBasesIfNeeded"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
          <div class="form-item-tip">选择要检索的知识库</div>
        </el-form-item>

        <el-divider content-position="left">检索配置</el-divider>

        <el-form-item label="查询文本" prop="query">
          <el-input
            v-model="formData.query"
            type="textarea"
            :rows="3"
            placeholder="请输入查询文本，支持变量替换"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-form-item label="Top-K" prop="topK">
          <el-input-number
            v-model="formData.topK"
            :min="1"
            :max="10"
            style="width: 100%"
          />
          <div class="form-item-tip">返回最相似的K个文档块，范围：1-10，默认5</div>
        </el-form-item>

        <el-form-item label="相似度阈值" prop="similarityThreshold">
          <el-slider
            v-model="formData.similarityThreshold"
            :min="0"
            :max="1"
            :step="0.01"
            show-input
            :show-input-controls="false"
            style="width: 100%"
          />
          <div class="form-item-tip">范围：0-1，默认0.7</div>
        </el-form-item>
      </template>

      <!-- 意图识别节点配置 -->
      <template v-else-if="nodeType === 'intent'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="输入文本" prop="inputText">
          <el-input
            v-model="formData.inputText"
            type="textarea"
            :rows="3"
            placeholder="请输入要识别的文本，支持变量替换"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-form-item label="意图类别" prop="intentCategories">
          <el-select
            v-model="formData.intentCategories"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请输入或选择意图类别"
            style="width: 100%"
          >
            <el-option
              v-for="category in formData.intentCategories"
              :key="category"
              :label="category"
              :value="category"
            />
          </el-select>
          <div class="form-item-tip">可以输入新的意图类别，多个类别用逗号分隔</div>
        </el-form-item>

        <el-divider content-position="left">识别配置</el-divider>

        <el-form-item label="识别方式" prop="recognitionMethod">
          <el-radio-group v-model="formData.recognitionMethod">
            <el-radio label="llm">LLM识别</el-radio>
            <el-radio label="keyword">关键词匹配</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          v-if="formData.recognitionMethod === 'llm'"
          label="智能体UUID"
          prop="agentUuid"
        >
          <el-select
            v-model="formData.agentUuid"
            placeholder="请选择智能体"
            filterable
            clearable
            style="width: 100%"
            :loading="loadingAgents"
            @visible-change="loadAgentsIfNeeded"
          >
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="formData.recognitionMethod === 'keyword'"
          label="关键词映射"
          prop="keywords"
        >
          <div class="keywords-editor">
            <div
              v-for="(intentKey, index) in keywordIntentKeys"
              :key="index"
              class="keyword-item"
            >
              <el-input
                v-model="keywordIntentKeys[index]"
                placeholder="意图名称"
                style="width: 30%"
                @input="updateKeywordIntentKey(index, $event)"
              />
              <el-select
                v-model="formData.keywords[keywordIntentKeys[index]]"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="关键词列表"
                style="width: 70%"
              />
              <el-button
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="removeKeywordIntent(keywordIntentKeys[index])"
              />
            </div>
            <el-button
              type="primary"
              :icon="Plus"
              size="small"
              @click="addKeywordIntent"
            >
              添加意图关键词
            </el-button>
          </div>
        </el-form-item>
      </template>

      <!-- 字符串处理节点配置 -->
      <template v-else-if="nodeType === 'string'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-divider content-position="left">操作配置</el-divider>

        <el-form-item label="操作类型" prop="operation">
          <el-select
            v-model="formData.operation"
            placeholder="请选择操作类型"
            style="width: 100%"
          >
            <el-option label="拼接 (concat)" value="concat" />
            <el-option label="替换 (replace)" value="replace" />
            <el-option label="截取 (substring)" value="substring" />
            <el-option label="格式化 (format)" value="format" />
            <el-option label="去空格 (trim)" value="trim" />
            <el-option label="转大写 (upper)" value="upper" />
            <el-option label="转小写 (lower)" value="lower" />
          </el-select>
        </el-form-item>

        <el-form-item label="输入字符串" prop="inputString">
          <el-input
            v-model="formData.inputString"
            type="textarea"
            :rows="3"
            placeholder="请输入字符串，支持变量替换"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-divider content-position="left">操作参数</el-divider>

        <!-- 根据操作类型显示不同的参数配置 -->
        <template v-if="formData.operation === 'concat'">
          <el-form-item label="分隔符" prop="parameters.separator">
            <el-input
              v-model="formData.parameters.separator"
              placeholder="分隔符（可选，默认为空）"
            />
            <div class="form-item-tip">用于分隔多个字符串的分隔符</div>
          </el-form-item>
          <el-form-item label="拼接字符串列表" prop="parameters.strings">
            <div class="key-value-editor">
              <div
                v-for="(str, index) in concatStrings"
                :key="index"
                class="key-value-item"
              >
                <el-input
                  v-model="concatStrings[index]"
                  :placeholder="`字符串 ${index + 1}`"
                  style="width: calc(100% - 40px)"
                />
                <el-button
                  type="danger"
                  :icon="Delete"
                  circle
                  size="small"
                  @click="removeConcatString(index)"
                />
              </div>
              <el-button
                type="primary"
                :icon="Plus"
                size="small"
                @click="addConcatString"
              >
                添加字符串
              </el-button>
            </div>
            <div class="form-item-tip">要拼接的字符串列表，将按顺序用分隔符连接</div>
          </el-form-item>
        </template>

        <template v-else-if="formData.operation === 'replace'">
          <el-form-item label="查找字符串" prop="parameters.target">
            <el-input
              v-model="formData.parameters.target"
              placeholder="要查找的字符串"
            />
            <div class="form-item-tip">在输入字符串中要查找并替换的文本</div>
          </el-form-item>
          <el-form-item label="替换为" prop="parameters.replacement">
            <el-input
              v-model="formData.parameters.replacement"
              placeholder="替换为的字符串"
            />
            <div class="form-item-tip">用于替换查找字符串的文本</div>
          </el-form-item>
        </template>

        <template v-else-if="formData.operation === 'substring'">
          <el-form-item label="起始位置" prop="parameters.start">
            <el-input-number
              v-model="formData.parameters.start"
              :min="-9999"
              style="width: 100%"
            />
            <div class="form-item-tip">支持负数索引，-1表示最后一个字符</div>
          </el-form-item>
          <el-form-item label="结束位置" prop="parameters.end">
            <el-input-number
              v-model="formData.parameters.end"
              :min="-9999"
              style="width: 100%"
            />
            <div class="form-item-tip">可选，不填则截取到末尾。支持负数索引</div>
          </el-form-item>
        </template>

        <template v-else-if="formData.operation === 'format'">
          <el-form-item label="格式化值映射" prop="parameters.values">
            <div class="key-value-editor">
              <div
                v-for="(value, key, index) in formatValues"
                :key="index"
                class="key-value-item"
              >
                <el-input
                  v-model="formatKeys[index]"
                  placeholder="占位符key（如：name）"
                  style="width: 40%"
                  @input="updateFormatKey(index, $event)"
                />
                <el-input
                  v-model="formatValues[key]"
                  placeholder="替换值，支持变量替换"
                  style="width: 60%"
                />
                <el-button
                  type="danger"
                  :icon="Delete"
                  circle
                  size="small"
                  @click="removeFormatValue(key)"
                />
              </div>
              <el-button
                type="primary"
                :icon="Plus"
                size="small"
                @click="addFormatValue"
              >
                添加格式化值
              </el-button>
            </div>
            <div class="form-item-tip">
              在输入字符串中使用双大括号占位符，如：Hello {{name}}，您有 {{count}} 条消息
            </div>
          </el-form-item>
        </template>
      </template>

      <el-divider />

      <div style="display: flex; gap: 12px; margin-top: 20px;">
        <el-button @click="handleClose" style="flex: 1;">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving" style="flex: 1;">
          保存
        </el-button>
      </div>
    </el-form>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getAgentList } from '@/api/agent'
import { getKnowledgeBaseList } from '@/api/knowledgeBase'
import type { Agent } from '@/types/entity'
import type { KnowledgeBase } from '@/api/knowledgeBase'
import type { WorkflowNode } from '@/api/workflow'

interface Props {
  modelValue: boolean
  node: WorkflowNode | null
  availableNodes?: WorkflowNode[] // 用于变量替换提示
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', config: any): void
}

const props = withDefaults(defineProps<Props>(), {
  availableNodes: () => []
})

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const nodeType = computed(() => props.node?.type || '')
const dialogTitle = computed(() => {
  const nodeName = props.node?.label || '节点'
  return `配置 ${nodeName}`
})

const saving = ref(false)

// 表单数据
const formData = ref<any>({})
const headerKeys = ref<string[]>([])
const keywordIntentKeys = ref<string[]>([])
const bodyString = ref('')

// 字符串处理节点相关
const concatStrings = ref<string[]>([])
const formatKeys = ref<string[]>([])
const formatValues = ref<Record<string, string>>({})

// 智能体列表
const agents = ref<Agent[]>([])
const loadingAgents = ref(false)
const agentsLoaded = ref(false)

// 知识库列表
const knowledgeBases = ref<KnowledgeBase[]>([])
const loadingKnowledgeBases = ref(false)
const knowledgeBasesLoaded = ref(false)

// 表单验证规则
const formRules = computed<FormRules>(() => {
  const rules: FormRules = {}
  
  if (nodeType.value === 'llm') {
    rules.agentUuid = [
      { required: true, message: '请选择智能体', trigger: 'change' }
    ]
    rules.prompt = [
      { required: true, message: '请输入提示词', trigger: 'blur' }
    ]
    rules.temperature = [
      { type: 'number', min: 0, max: 2, message: '温度参数必须在0-2之间', trigger: 'blur' }
    ]
    rules.maxTokens = [
      { type: 'number', min: 1, message: '最大Token数必须大于0', trigger: 'blur' }
    ]
  } else if (nodeType.value === 'http') {
    rules.url = [
      { required: true, message: '请输入请求URL', trigger: 'blur' }
    ]
  } else if (nodeType.value === 'knowledge') {
    rules.knowledgeBaseId = [
      { required: true, message: '请选择知识库', trigger: 'change' }
    ]
    rules.query = [
      { required: true, message: '请输入查询文本', trigger: 'blur' }
    ]
    rules.topK = [
      { type: 'number', min: 1, max: 10, message: 'Top-K必须在1-10之间', trigger: 'blur' }
    ]
    rules.similarityThreshold = [
      { type: 'number', min: 0, max: 1, message: '相似度阈值必须在0-1之间', trigger: 'blur' }
    ]
  } else if (nodeType.value === 'intent') {
    rules.inputText = [
      { required: true, message: '请输入输入文本', trigger: 'blur' }
    ]
    rules.intentCategories = [
      { required: true, message: '请输入意图类别', trigger: 'change' }
    ]
    rules.agentUuid = [
      { 
        validator: (rule, value, callback) => {
          if (formData.value.recognitionMethod === 'llm' && !value) {
            callback(new Error('使用LLM识别方式时，请选择智能体'))
          } else {
            callback()
          }
        },
        trigger: 'change'
      }
    ]
    rules.keywords = [
      {
        validator: (rule, value, callback) => {
          if (formData.value.recognitionMethod === 'keyword' && (!value || Object.keys(value).length === 0)) {
            callback(new Error('使用关键词匹配方式时，请配置关键词映射'))
          } else {
            callback()
          }
        },
        trigger: 'change'
      }
    ]
  } else if (nodeType.value === 'string') {
    rules.operation = [
      { required: true, message: '请选择操作类型', trigger: 'change' }
    ]
    rules.inputString = [
      { required: true, message: '请输入输入字符串', trigger: 'blur' }
    ]
  }
  
  return rules
})

// 初始化表单数据
const initFormData = () => {
  if (!props.node) {
    formData.value = {}
    return
  }

  const config = props.node.config || {}
  const type = props.node.type

  // 根据节点类型初始化表单数据
  if (type === 'start' || type === 'end') {
    formData.value = {}
  } else if (type === 'llm') {
    formData.value = {
      agentUuid: config.agentUuid || '',
      prompt: config.prompt || '',
      temperature: config.temperature ?? 0.7,
      maxTokens: config.maxTokens ?? 2000
    }
  } else if (type === 'http') {
    formData.value = {
      url: config.url || '',
      method: config.method || 'GET',
      headers: config.headers || {},
      body: config.body || null
    }
    headerKeys.value = Object.keys(formData.value.headers)
    bodyString.value = config.body ? JSON.stringify(config.body, null, 2) : ''
  } else if (type === 'knowledge') {
    // 知识库ID可能是数字或字符串，统一处理为字符串
    const kbId = config.knowledgeBaseId
    formData.value = {
      knowledgeBaseId: kbId ? String(kbId) : null,
      query: config.query || '',
      topK: config.topK ?? 5,
      similarityThreshold: config.similarityThreshold ?? 0.7
    }
  } else if (type === 'intent') {
    formData.value = {
      inputText: config.inputText || '',
      intentCategories: config.intentCategories || [],
      recognitionMethod: config.recognitionMethod || 'llm',
      agentUuid: config.agentUuid || '',
      keywords: config.keywords || {}
    }
    keywordIntentKeys.value = Object.keys(formData.value.keywords)
  } else if (type === 'string') {
    formData.value = {
      operation: config.operation || '',
      inputString: config.inputString || '',
      parameters: config.parameters || {}
    }
    
    // 初始化concat字符串列表
    if (config.operation === 'concat' && config.parameters?.strings) {
      concatStrings.value = [...(config.parameters.strings as string[])]
    } else {
      concatStrings.value = []
    }
    
    // 初始化format值映射
    if (config.operation === 'format' && config.parameters?.values) {
      formatValues.value = { ...(config.parameters.values as Record<string, string>) }
      formatKeys.value = Object.keys(formatValues.value)
    } else {
      formatValues.value = {}
      formatKeys.value = []
    }
  } else {
    // 重置字符串处理相关数据
    concatStrings.value = []
    formatValues.value = {}
    formatKeys.value = []
  }
}

// 加载智能体列表
const loadAgents = async () => {
  if (agentsLoaded.value) return
  
  loadingAgents.value = true
  try {
    agents.value = await getAgentList()
    agentsLoaded.value = true
  } catch (error: any) {
    console.error('加载智能体列表失败:', error)
    ElMessage.error(error.message || '加载智能体列表失败')
  } finally {
    loadingAgents.value = false
  }
}

const loadAgentsIfNeeded = (visible: boolean) => {
  if (visible && !agentsLoaded.value) {
    loadAgents()
  }
}

// 解析知识库ID
// 后端KnowledgeNodeConfig期望Long类型，但知识库ID是String类型
// 如果ID是纯数字字符串，转换为数字；否则使用字符串（后端执行时会转换为String）
const parseKnowledgeBaseId = (id: string): number | string | null => {
  if (!id) return null
  // 尝试将字符串ID转换为数字
  const numId = Number(id)
  if (!isNaN(numId) && isFinite(numId) && numId > 0) {
    return numId
  }
  // 如果无法转换为数字，返回字符串ID（后端在执行时会转换为String）
  // 注意：这可能需要后端修改KnowledgeNodeConfig支持String类型
  return id
}

// 加载知识库列表
const loadKnowledgeBases = async () => {
  if (knowledgeBasesLoaded.value) return
  
  loadingKnowledgeBases.value = true
  try {
    const result = await getKnowledgeBaseList({ pageSize: 1000 })
    knowledgeBases.value = result.list || []
    knowledgeBasesLoaded.value = true
  } catch (error: any) {
    console.error('加载知识库列表失败:', error)
    ElMessage.error(error.message || '加载知识库列表失败')
  } finally {
    loadingKnowledgeBases.value = false
  }
}

const loadKnowledgeBasesIfNeeded = (visible: boolean) => {
  if (visible && !knowledgeBasesLoaded.value) {
    loadKnowledgeBases()
  }
}

// HTTP节点：请求头管理
const addHeader = () => {
  const key = `header_${Date.now()}`
  formData.value.headers[key] = ''
  headerKeys.value.push(key)
}

const removeHeader = (key: string) => {
  delete formData.value.headers[key]
  const index = headerKeys.value.indexOf(key)
  if (index > -1) {
    headerKeys.value.splice(index, 1)
  }
}

const updateHeaderKey = (index: number, newKey: string) => {
  const oldKey = headerKeys.value[index]
  if (oldKey && oldKey !== newKey) {
    const value = formData.value.headers[oldKey]
    delete formData.value.headers[oldKey]
    formData.value.headers[newKey] = value
    headerKeys.value[index] = newKey
  }
}

// HTTP节点：请求体处理
const handleBodyChange = () => {
  if (!bodyString.value.trim()) {
    formData.value.body = null
    return
  }
  
  try {
    formData.value.body = JSON.parse(bodyString.value)
  } catch (error) {
    ElMessage.warning('请求体格式不正确，请检查JSON格式')
  }
}

// 意图识别节点：关键词映射管理
const addKeywordIntent = () => {
  const intent = `intent_${Date.now()}`
  formData.value.keywords[intent] = []
  keywordIntentKeys.value.push(intent)
}

const removeKeywordIntent = (intent: string) => {
  delete formData.value.keywords[intent]
  const index = keywordIntentKeys.value.indexOf(intent)
  if (index > -1) {
    keywordIntentKeys.value.splice(index, 1)
  }
}

const updateKeywordIntentKey = (index: number, newKey: string) => {
  const oldKey = keywordIntentKeys.value[index]
  if (oldKey && oldKey !== newKey && newKey) {
    const keywords = formData.value.keywords[oldKey] || []
    delete formData.value.keywords[oldKey]
    formData.value.keywords[newKey] = keywords
    keywordIntentKeys.value[index] = newKey
  }
}

// 字符串处理节点：concat字符串管理
const addConcatString = () => {
  concatStrings.value.push('')
}

const removeConcatString = (index: number) => {
  concatStrings.value.splice(index, 1)
}

// 字符串处理节点：format值映射管理
const addFormatValue = () => {
  const key = `key_${Date.now()}`
  formatValues.value[key] = ''
  formatKeys.value.push(key)
}

const removeFormatValue = (key: string) => {
  delete formatValues.value[key]
  const index = formatKeys.value.indexOf(key)
  if (index > -1) {
    formatKeys.value.splice(index, 1)
  }
}

const updateFormatKey = (index: number, newKey: string) => {
  const oldKey = formatKeys.value[index]
  if (oldKey && oldKey !== newKey && newKey) {
    const value = formatValues.value[oldKey] || ''
    delete formatValues.value[oldKey]
    formatValues.value[newKey] = value
    formatKeys.value[index] = newKey
  }
}

// 保存配置
const handleSave = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    // 处理HTTP节点的headers（转换为正确的格式）
    if (nodeType.value === 'http') {
      const headers: Record<string, string> = {}
      headerKeys.value.forEach(key => {
        if (formData.value.headers[key]) {
          headers[key] = formData.value.headers[key]
        }
      })
      formData.value.headers = headers
    }

    // 处理意图识别节点的keywords（转换为正确的格式）
    if (nodeType.value === 'intent' && formData.value.recognitionMethod === 'keyword') {
      const keywords: Record<string, string[]> = {}
      keywordIntentKeys.value.forEach(intent => {
        if (intent && formData.value.keywords[intent] && formData.value.keywords[intent].length > 0) {
          keywords[intent] = formData.value.keywords[intent]
        }
      })
      formData.value.keywords = keywords
    }
    
    // 如果使用LLM方式，清空keywords
    if (nodeType.value === 'intent' && formData.value.recognitionMethod === 'llm') {
      delete formData.value.keywords
    }
    
    // 如果使用关键词方式，清空agentUuid
    if (nodeType.value === 'intent' && formData.value.recognitionMethod === 'keyword') {
      delete formData.value.agentUuid
    }

    // 处理知识库节点的knowledgeBaseId（转换为数字，如果是纯数字字符串）
    if (nodeType.value === 'knowledge' && formData.value.knowledgeBaseId) {
      const kbId = formData.value.knowledgeBaseId
      const numId = Number(kbId)
      // 如果ID是纯数字字符串，转换为数字；否则保持字符串
      if (!isNaN(numId) && isFinite(numId) && numId > 0) {
        formData.value.knowledgeBaseId = numId
      }
      // 否则保持字符串（后端可能需要修改以支持String类型）
    }

    // 处理字符串处理节点的parameters
    if (nodeType.value === 'string') {
      const operation = formData.value.operation
      const parameters: Record<string, any> = {}
      
      if (operation === 'concat') {
        // concat操作：需要strings数组和separator
        parameters.separator = formData.value.parameters?.separator || ''
        parameters.strings = concatStrings.value.filter(s => s && s.trim())
      } else if (operation === 'replace') {
        // replace操作：需要target和replacement
        parameters.target = formData.value.parameters?.target || ''
        parameters.replacement = formData.value.parameters?.replacement || ''
      } else if (operation === 'substring') {
        // substring操作：需要start和end（可选）
        if (formData.value.parameters?.start !== undefined) {
          parameters.start = formData.value.parameters.start
        }
        if (formData.value.parameters?.end !== undefined) {
          parameters.end = formData.value.parameters.end
        }
      } else if (operation === 'format') {
        // format操作：需要values对象
        const values: Record<string, string> = {}
        formatKeys.value.forEach(key => {
          if (key && formatValues.value[key] !== undefined) {
            values[key] = formatValues.value[key]
          }
        })
        parameters.values = values
      }
      // trim、upper、lower操作不需要parameters
      
      formData.value.parameters = parameters
    }

    // 发送保存事件
    emit('save', { ...formData.value })
    ElMessage.success('配置保存成功')
    visible.value = false
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}

// 监听节点变化，重新初始化表单
watch(() => props.node, () => {
  if (props.node) {
    initFormData()
  }
}, { immediate: true })

// 监听对话框显示状态
watch(visible, (newVal) => {
  if (newVal && props.node) {
    initFormData()
  }
})
</script>

<style scoped>
/* 配置内容容器 */
.config-content {
  padding: 0;
  height: 100%;
  overflow-y: auto;
}

.config-content::-webkit-scrollbar {
  width: 8px;
}

.config-content::-webkit-scrollbar-track {
  background: #f1f3f5;
  border-radius: 4px;
}

.config-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
  transition: background 0.3s ease;
}

.config-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 抽屉样式优化 */
:deep(.el-drawer) {
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-drawer__header) {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

:deep(.el-drawer__title) {
  font-weight: 600;
  font-size: 18px;
  color: #303133;
}

/* 表单样式优化 */
:deep(.el-form) {
  padding: 0;
}

:deep(.el-form-item__label) {
  color: #374151;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  padding: 0;
}

:deep(.el-divider) {
  margin: 20px 0;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.2);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

:deep(.el-textarea__inner:hover) {
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.2);
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-radio-group) {
  display: flex;
  gap: 16px;
}

:deep(.el-radio) {
  margin-right: 0;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #667eea;
  border-color: #667eea;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 600;
  padding: 10px 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

:deep(.el-button--primary:active) {
  transform: translateY(0);
}

:deep(.el-alert) {
  border-radius: 10px;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

:deep(.el-alert--info) {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  color: #4338ca;
}

:deep(.el-slider) {
  margin-top: 8px;
}

:deep(.el-slider__runway) {
  background-color: #e5e7eb;
  border-radius: 4px;
}

:deep(.el-slider__bar) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
}

:deep(.el-slider__button) {
  border: 3px solid #667eea;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

:deep(.el-slider__button:hover) {
  transform: scale(1.2);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.form-item-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  line-height: 1.5;
}

.key-value-editor {
  width: 100%;
  background: #f9fafb;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.key-value-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.key-value-item:hover {
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.15);
  transform: translateX(2px);
}

.key-value-item:last-child {
  margin-bottom: 0;
}

.keywords-editor {
  width: 100%;
  background: #f9fafb;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.keyword-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.keyword-item:hover {
  box-shadow: 0 2px 6px rgba(236, 72, 153, 0.15);
  transform: translateX(2px);
}

.keyword-item:last-child {
  margin-bottom: 0;
}
</style>

