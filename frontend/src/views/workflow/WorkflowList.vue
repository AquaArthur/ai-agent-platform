<template>
  <div class="workflow-list-container">
    <div class="page-header">
      <h2>工作流管理</h2>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input
            v-model="searchQuery"
            placeholder="搜索工作流名称或描述"
            clearable
            :prefix-icon="Search"
            @input="handleSearch"
          />
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="filterStatus"
            placeholder="状态筛选"
            clearable
            @change="loadWorkflows"
            style="width: 100%"
          >
            <el-option label="有效" :value="true" />
            <el-option label="无效" :value="false" />
          </el-select>
        </el-col>
        <el-col :span="10" style="text-align: right;">
          <el-button @click="resetFilters">重置筛选</el-button>
          <el-button type="primary" :icon="Plus" @click="createWorkflow">
            创建工作流
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 工作流列表 -->
    <div class="workflows-table">
      <el-table :data="workflows" style="width: 100%" v-loading="loading">
        <template #empty>
          <el-empty description="暂无工作流数据" />
        </template>
        <el-table-column prop="name" label="工作流名称" min-width="200">
          <template #default="{ row }">
            <div v-if="editingRowId === row.uuid" class="inline-edit-cell">
              <el-input
                v-model="editingName"
                ref="editInputRef"
                size="small"
                @blur="handleSaveName(row)"
                @keyup.enter="handleSaveName(row)"
                @keyup.esc="handleCancelEdit"
                maxlength="100"
                show-word-limit
              />
            </div>
            <div v-else class="editable-name" @dblclick="handleStartEdit(row)">
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isValid ? 'success' : 'info'" size="small">
              {{ row.isValid ? '有效' : '无效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="节点数" width="100">
          <template #default="{ row }">
            {{ row.nodes?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime || row.create_time) }}
          </template>
        </el-table-column>
        <el-table-column label="关联智能体" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.agentId" type="success" size="small">已关联</el-tag>
            <el-tag v-else type="info" size="small">未关联</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editWorkflow(row)">编辑</el-button>
            <el-tooltip
              v-if="!row.agentId"
              content="该工作流未关联智能体，请先在智能体配置中关联此工作流"
              placement="top"
            >
              <el-button type="success" size="small" disabled>执行</el-button>
            </el-tooltip>
            <el-button v-else type="success" size="small" @click="executeWorkflow(row)">执行</el-button>
            <el-button type="info" size="small" @click="viewExecutions(row)">执行历史</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="loadWorkflows"
      @current-change="loadWorkflows"
      style="margin-top: 20px; justify-content: flex-end;"
    />

    <!-- 执行对话框 -->
    <el-dialog
      v-model="executeDialogVisible"
      title="执行工作流"
      width="600px"
      @close="handleExecuteDialogClose"
    >
      <el-form :model="executeForm" label-width="100px">
        <el-form-item v-if="needsLlmModel" label="LLM模型" required>
          <el-select
            v-model="executeForm.llmModelId"
            placeholder="请选择LLM模型"
            style="width: 100%"
            :loading="loadingModels"
            filterable
          >
            <el-option
              v-for="model in llmModels"
              :key="model.id"
              :label="model.displayName || model.name"
              :value="model.id"
            >
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ model.displayName || model.name }}</span>
                <el-tag v-if="model.isDefault" type="success" size="small" style="margin-left: 8px;">默认</el-tag>
              </div>
            </el-option>
          </el-select>
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            此工作流包含LLM节点或意图识别节点，需要选择LLM模型
          </div>
        </el-form-item>
        <el-form-item label="输入参数">
          <div class="input-param-section">
            <!-- 默认提示信息 -->
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="input-tip-alert"
            >
              <template #title>
                <span class="tip-text">{{ inputTipText }}</span>
              </template>
            </el-alert>
            <div class="textarea-wrapper">
              <el-input
                v-model="executeForm.inputJson"
                type="textarea"
                :rows="8"
                placeholder='请输入JSON格式的输入参数，例如：{"query": "你好"}'
                class="json-input"
              />
              <div class="action-buttons">
                <el-button type="primary" size="small" @click="checkJsonFormat" :icon="CircleCheck">
                  JSON格式检查
                </el-button>
                <el-button size="small" @click="formatJson" :icon="MagicStick">
                  排版
                </el-button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmExecute" :loading="executing">执行</el-button>
      </template>
    </el-dialog>

    <!-- 执行历史对话框 -->
    <el-dialog
      v-model="executionsDialogVisible"
      :title="`执行历史${currentWorkflowForExecutions ? ' - ' + currentWorkflowForExecutions.name : ''}`"
      width="80%"
    >
      <!-- 筛选和搜索 -->
      <div class="execution-filter" style="margin-bottom: 16px;">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-select
              v-model="executionFilterStatus"
              placeholder="状态筛选"
              clearable
              @change="loadExecutions"
              style="width: 100%"
            >
              <el-option label="等待中" value="pending" />
              <el-option label="执行中" value="running" />
              <el-option label="已完成" value="completed" />
              <el-option label="失败" value="failed" />
              <el-option label="已终止" value="terminated" />
            </el-select>
          </el-col>
        </el-row>
      </div>

      <el-table :data="executions" style="width: 100%" v-loading="loadingExecutions">
        <el-table-column label="执行ID" width="200">
          <template #default="{ row }">
            {{ row.executionId || row.execution_id || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getWorkflowStatusType(row.status)" size="small">
              {{ getWorkflowStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行时间(ms)" width="150">
          <template #default="{ row }">
            {{ getField(row, 'executionTime', 'execution_time', 0) }}
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDate(getField(row, 'startedAt', 'started_at', null)) }}
          </template>
        </el-table-column>
        <el-table-column label="完成时间" width="180">
          <template #default="{ row }">
            {{ getField(row, 'completedAt', 'completed_at', null) ? formatDate(getField(row, 'completedAt', 'completed_at', null)) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewExecutionDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="executionCurrentPage"
        v-model:page-size="executionPageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="executionTotal"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadExecutions"
        @current-change="loadExecutions"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-dialog>

    <!-- 执行详情对话框 -->
    <el-dialog
      v-model="executionDetailVisible"
      title="执行详情"
      width="70%"
      class="execution-detail-dialog"
    >
      <div v-if="executionDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="执行ID">{{ getField(executionDetail, 'executionId', 'execution_id', '-') }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getWorkflowStatusType(executionDetail.status)" size="small">
              {{ getWorkflowStatusText(executionDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="执行时间">
            {{ getField(executionDetail, 'executionTime', 'execution_time', 0) }}ms
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ formatDate(getField(executionDetail, 'startedAt', 'started_at', null)) }}
          </el-descriptions-item>
          <el-descriptions-item label="完成时间" :span="2">
            {{ getField(executionDetail, 'completedAt', 'completed_at', null) ? formatDate(getField(executionDetail, 'completedAt', 'completed_at', null)) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="输入参数" :span="2">
            <pre class="json-display">{{ JSON.stringify(executionDetail.input || {}, null, 2) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="输出结果" :span="2">
            <pre class="json-display">{{ JSON.stringify(executionDetail.output || {}, null, 2) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="getField(executionDetail, 'errorMessage', 'error_message', null)" label="错误信息" :span="2">
            <el-alert type="error" :closable="false">{{ getField(executionDetail, 'errorMessage', 'error_message', '') }}</el-alert>
          </el-descriptions-item>
        </el-descriptions>
        
        <!-- 节点执行记录 -->
        <div v-if="executionDetail && (getField(executionDetail, 'nodeExecutions', 'node_executions', []) || []).length > 0" class="node-executions-section" style="margin-top: 20px;">
          <h4>节点执行记录</h4>
          <el-table :data="(getField(executionDetail, 'nodeExecutions', 'node_executions', []) || [])" style="width: 100%">
            <el-table-column label="节点ID" width="150">
              <template #default="{ row }">
                {{ getField(row, 'nodeId', 'node_id', '-') }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getWorkflowStatusType(row.status)" size="small">
                  {{ getWorkflowStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="开始时间" width="180">
              <template #default="{ row }">
                {{ row.startedAt || row.started_at ? formatDate(row.startedAt || row.started_at) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="完成时间" width="180">
              <template #default="{ row }">
                {{ row.completedAt || row.completed_at ? formatDate(row.completedAt || row.completed_at) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="输出" min-width="200">
              <template #default="{ row }">
                <pre class="node-output">{{ JSON.stringify(row.output || {}, null, 2) }}</pre>
              </template>
            </el-table-column>
            <el-table-column label="错误信息" min-width="200" v-if="executionDetail && (getField(executionDetail, 'nodeExecutions', 'node_executions', []) || []).some((n: any) => getField(n, 'error', 'error_message', null))">
              <template #default="{ row }">
                <span v-if="getField(row, 'error', 'error_message', null)" style="color: #f56c6c;">
                  {{ getField(row, 'error', 'error_message', '') }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, CircleCheck, MagicStick } from '@element-plus/icons-vue'
import {
  getWorkflowList,
  deleteWorkflow,
  updateWorkflow,
  executeWorkflow as executeWorkflowAPI,
  getWorkflowExecutions,
  getExecution
} from '@/api/workflow'
import type { Workflow, WorkflowExecution } from '@/api/workflow'
import { getLlmModelList } from '@/api/llm'
import type { LlmModel } from '@/types/entity'
import { formatDate, getWorkflowStatusType, getWorkflowStatusText } from '@/utils/formatters'
import { checkIfNeedsLlmModel, getDefaultWorkflowInput, getField } from '@/utils/workflow'

const router = useRouter()

// 数据
const loading = ref(false)
const workflows = ref<Workflow[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const filterStatus = ref<boolean | null>(null)

// 执行相关
const executeDialogVisible = ref(false)
const executeForm = ref({
  inputJson: '{}',
  llmModelId: ''
})
const executing = ref(false)
const currentWorkflow = ref<Workflow | null>(null)
const inputTipText = ref('请输入JSON格式的输入参数，例如：{"query": "你好"}')
const llmModels = ref<LlmModel[]>([])
const loadingModels = ref(false)
const needsLlmModel = ref(false)

// 执行历史
const executionsDialogVisible = ref(false)
const executions = ref<WorkflowExecution[]>([])
const executionDetailVisible = ref(false)
const executionDetail = ref<WorkflowExecution | null>(null)
const executionCurrentPage = ref(1)
const executionPageSize = ref(10)
const executionTotal = ref(0)
const executionFilterStatus = ref<string | null>(null)
const loadingExecutions = ref(false)
const currentWorkflowForExecutions = ref<Workflow | null>(null)

// 内联编辑相关
const editingRowId = ref<string | null>(null)
const editingName = ref('')
const originalName = ref('')
const editInputRef = ref<any>(null)
const renaming = ref(false)

// 加载工作流列表
const loadWorkflows = async () => {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchQuery.value) {
      params.search = searchQuery.value
    }
    
    const response = await getWorkflowList(params)
    
    // 确保响应数据正确解析
    if (response && typeof response === 'object') {
      workflows.value = Array.isArray(response.items) ? response.items : []
      total.value = typeof response.total === 'number' ? response.total : 0
    } else {
      workflows.value = []
      total.value = 0
    }
    
    // 如果有状态筛选，在前端过滤
    if (filterStatus.value !== null) {
      workflows.value = workflows.value.filter(w => w.isValid === filterStatus.value)
    }
  } catch (error: any) {
    ElMessage.error('加载工作流列表失败: ' + (error.message || '未知错误'))
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadWorkflows()
}

// 重置筛选
const resetFilters = () => {
  searchQuery.value = ''
  filterStatus.value = null
  currentPage.value = 1
  loadWorkflows()
}

// 创建工作流
const createWorkflow = () => {
  router.push('/main/workflow-editor')
}

// 编辑工作流
const editWorkflow = (workflow: Workflow) => {
  router.push(`/main/workflow-editor/${workflow.uuid}`)
}


// 加载LLM模型列表
const loadLlmModels = async () => {
  if (llmModels.value.length > 0) return
  
  loadingModels.value = true
  try {
    const models = await getLlmModelList()
    // 只显示激活的模型，并按isDefault和sortOrder排序
    llmModels.value = models
      .filter(model => model.isActive !== false)
      .sort((a, b) => {
        // 默认模型优先
        if (a.isDefault && !b.isDefault) return -1
        if (!a.isDefault && b.isDefault) return 1
        // 然后按sortOrder排序
        return (a.sortOrder || 0) - (b.sortOrder || 0)
      })
  } catch (error: any) {
    console.error('加载LLM模型列表失败:', error)
    ElMessage.error('加载LLM模型列表失败: ' + (error.message || '未知错误'))
  } finally {
    loadingModels.value = false
  }
}


// 执行工作流
const executeWorkflow = async (workflow: Workflow) => {
  currentWorkflow.value = workflow
  
  // 检查是否需要LLM模型
  needsLlmModel.value = checkIfNeedsLlmModel(workflow)
  
  // 如果需要LLM模型，加载模型列表
  if (needsLlmModel.value) {
    await loadLlmModels()
    // 设置默认模型（优先选择isDefault为true的模型，否则选择第一个）
    const defaultModel = llmModels.value.find(m => m.isDefault) || llmModels.value[0]
    if (defaultModel?.id) {
      executeForm.value.llmModelId = defaultModel.id!
    }
  } else {
    executeForm.value.llmModelId = ''
  }
  
  const defaultInput = getDefaultWorkflowInput(workflow)
  executeForm.value.inputJson = defaultInput.json
  inputTipText.value = defaultInput.tip
  executeDialogVisible.value = true
}

// 处理执行对话框关闭
const handleExecuteDialogClose = () => {
  // 重置表单
  executeForm.value = {
    inputJson: '{}',
    llmModelId: ''
  }
  needsLlmModel.value = false
  currentWorkflow.value = null
}

// JSON格式检查
const checkJsonFormat = () => {
  const jsonStr = executeForm.value.inputJson.trim()
  
  if (!jsonStr) {
    ElMessage.warning('请输入JSON内容')
    return
  }
  
  try {
    JSON.parse(jsonStr)
    ElMessage.success('JSON格式正确')
  } catch (error: any) {
    ElMessage.error('JSON格式错误: ' + error.message)
  }
}

// JSON排版（格式化）
const formatJson = () => {
  const jsonStr = executeForm.value.inputJson.trim()
  
  if (!jsonStr) {
    ElMessage.warning('请输入JSON内容')
    return
  }
  
  try {
    const parsed = JSON.parse(jsonStr)
    executeForm.value.inputJson = JSON.stringify(parsed, null, 2)
    ElMessage.success('JSON格式化成功')
  } catch (error: any) {
    ElMessage.error('JSON格式错误，无法格式化: ' + error.message)
  }
}

// 确认执行
const confirmExecute = async () => {
  if (!currentWorkflow.value) return
  
  // 检查工作流是否有关联的智能体
  if (!currentWorkflow.value.agentId) {
    ElMessage.warning('该工作流未关联智能体，无法执行。请先在智能体配置中关联此工作流。')
    return
  }
  
  // 检查工作流是否有id
  if (!currentWorkflow.value.id) {
    ElMessage.error('工作流ID不存在')
    return
  }
  
  // 如果需要LLM模型但未选择，提示用户
  if (needsLlmModel.value && !executeForm.value.llmModelId) {
    ElMessage.warning('请选择LLM模型')
    return
  }
  
  try {
    // 解析输入JSON，确保是对象格式
    let input: Record<string, any> = {}
    const inputJsonStr = executeForm.value.inputJson.trim()
    
    if (inputJsonStr) {
      try {
        const parsed = JSON.parse(inputJsonStr)
        // 确保input是对象类型，不是数组或其他类型
        if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
          input = parsed
        } else {
          ElMessage.error('输入参数必须是JSON对象格式，不能是数组或其他类型')
          return
        }
      } catch (e) {
        ElMessage.error('输入参数格式错误，请输入有效的JSON对象')
        return
      }
    }
    
    executing.value = true
    
    // 按照后端API要求的格式构建请求
    // API: POST /api/v1/workflows/execute?agentId=xxx&workflowId=xxx
    // 请求体: { "input": {...}, "llm_model_id": "..." }
    const request: Record<string, any> = {
      input: input
    }
    
    // 如果需要LLM模型，添加llm_model_id字段
    if (needsLlmModel.value && executeForm.value.llmModelId) {
      request.llm_model_id = executeForm.value.llmModelId
    }
    
    const response = await executeWorkflowAPI(
      currentWorkflow.value.agentId,
      currentWorkflow.value.id,
      request
    )
    
    ElMessage.success('工作流执行已提交')
    executeDialogVisible.value = false
    
    // 跳转到执行详情
    const execId = getField(response, 'executionId', 'execution_id', null)
    if (execId) {
      viewExecutionDetail({ executionId: execId, status: 'pending' } as WorkflowExecution)
    }
  } catch (error: any) {
    if (error.message?.includes('JSON') || error.message?.includes('格式')) {
      ElMessage.error('输入参数格式错误: ' + error.message)
    } else {
      ElMessage.error('执行工作流失败: ' + (error.message || '未知错误'))
    }
  } finally {
    executing.value = false
  }
}

// 查看执行历史
const viewExecutions = async (workflow: Workflow) => {
  currentWorkflowForExecutions.value = workflow
  executionCurrentPage.value = 1
  executionFilterStatus.value = null
  executionsDialogVisible.value = true
  await loadExecutions()
}

// 加载执行历史
const loadExecutions = async () => {
  if (!currentWorkflowForExecutions.value?.uuid) return
  
  loadingExecutions.value = true
  try {
    const response = await getWorkflowExecutions(currentWorkflowForExecutions.value!.uuid!, {
      page: executionCurrentPage.value,
      pageSize: executionPageSize.value,
      status: executionFilterStatus.value || undefined
    })
    executions.value = response.items || []
    executionTotal.value = response.total || 0
  } catch (error: any) {
    ElMessage.error('加载执行历史失败: ' + (error.message || '未知错误'))
    console.error(error)
  } finally {
    loadingExecutions.value = false
  }
}

// 查看执行详情
const viewExecutionDetail = async (execution: WorkflowExecution) => {
  try {
    const execId = getField(execution, 'executionId', 'execution_id', null)
    if (!execId) {
      ElMessage.error('执行ID不存在')
      return
    }
    const response = await getExecution(execId)
    executionDetail.value = response
    executionDetailVisible.value = true
  } catch (error: any) {
    ElMessage.error('加载执行详情失败: ' + (error.message || '未知错误'))
    console.error(error)
  }
}

// 开始编辑名称
const handleStartEdit = (workflow: Workflow) => {
  if (!workflow.uuid) return
  editingRowId.value = workflow.uuid
  editingName.value = workflow.name || ''
  originalName.value = workflow.name || ''
  
  // 聚焦输入框
  nextTick(() => {
    editInputRef.value?.focus()
    editInputRef.value?.select()
  })
}

// 保存名称
const handleSaveName = async (workflow: Workflow) => {
  if (!workflow.uuid || editingRowId.value !== workflow.uuid) return
  
  const newName = editingName.value.trim()
  
  // 如果名称没有变化，直接取消编辑
  if (newName === originalName.value) {
    handleCancelEdit()
    return
  }
  
  // 验证名称不能为空
  if (!newName) {
    ElMessage.warning('工作流名称不能为空')
    editingName.value = originalName.value
    handleCancelEdit()
    return
  }
  
  renaming.value = true
  try {
    // 获取完整的工作流数据，只更新名称
    const workflowToUpdate: Workflow = {
      ...workflow,
      name: newName
    }
    
    await updateWorkflow(workflow.uuid, workflowToUpdate)
    ElMessage.success('重命名成功')
    
    // 更新本地数据
    const index = workflows.value.findIndex(w => w.uuid === workflow.uuid)
    if (index !== -1 && workflows.value[index]) {
      workflows.value[index].name = newName
    }
    
    handleCancelEdit()
  } catch (error: any) {
    ElMessage.error('重命名失败: ' + (error.message || '未知错误'))
    editingName.value = originalName.value
    handleCancelEdit()
    console.error(error)
  } finally {
    renaming.value = false
  }
}

// 取消编辑
const handleCancelEdit = () => {
  editingRowId.value = null
  editingName.value = ''
  originalName.value = ''
}

// 删除工作流
const handleDelete = async (workflow: Workflow) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除工作流 "${workflow.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteWorkflow(workflow.uuid!)
    ElMessage.success('删除成功')
    loadWorkflows()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

// 直接使用导入的工具函数

onMounted(() => {
  loadWorkflows()
})
</script>

<style scoped>
.workflow-list-container {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: var(--gradient-bg-primary);
}

/* 使用公共样式类 */

.workflows-table {
  margin-bottom: 20px;
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
  margin: 0;
}

/* 执行详情对话框样式 */
.execution-detail-dialog :deep(.el-descriptions__body) {
  table-layout: fixed;
  width: 100%;
}

.execution-detail-dialog :deep(.el-descriptions__cell) {
  word-break: break-word;
}

.execution-detail-dialog .json-display {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  margin: 0;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
  width: 100%;
  box-sizing: border-box;
  display: block;
  overflow-x: hidden;
}

.node-executions-section {
  margin-top: 20px;
}

.node-executions-section h4 {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.node-output {
  background: #fafafa;
  padding: 8px;
  border-radius: 4px;
  margin: 0;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 11px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
  max-width: 300px;
  overflow-x: auto;
}

.editable-name {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
  user-select: none;
}

.editable-name:hover {
  background-color: #f5f7fa;
}

.inline-edit-cell {
  width: 100%;
}

/* 执行对话框样式 */
.input-param-section {
  width: 100%;
}

.input-tip-alert {
  margin-bottom: 16px;
}

.tip-text {
  font-size: 13px;
  line-height: 1.5;
}

.textarea-wrapper {
  position: relative;
}

.json-input {
  width: 100%;
}

.json-input :deep(.el-textarea__inner) {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  justify-content: flex-end;
}
</style>

