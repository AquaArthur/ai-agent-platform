<template>
  <div class="workflow-list-container">
    <div class="page-header">
      <h2>工作流管理</h2>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <el-row :gutter="16">
        <el-col :span="10">
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
        <el-col :span="8" style="text-align: right;">
          <el-button @click="resetFilters">重置筛选</el-button>
          <el-button type="primary" :icon="Plus" @click="createWorkflow">
            创建工作流
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 工作流列表 -->
    <div v-loading="loading" class="workflows-table">
      <el-table :data="workflows" style="width: 100%">
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
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
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
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editWorkflow(row)">编辑</el-button>
            <el-button type="success" size="small" @click="executeWorkflow(row)">执行</el-button>
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
      width="500px"
    >
      <el-form :model="executeForm" label-width="100px">
        <el-form-item label="输入参数">
          <el-input
            v-model="executeForm.inputJson"
            type="textarea"
            :rows="6"
            placeholder='请输入JSON格式的输入参数，例如：{"query": "你好"}'
          />
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
      title="执行历史"
      width="80%"
    >
      <!-- 筛选和搜索 -->
      <div class="execution-filter" style="margin-bottom: 16px;">
        <el-row :gutter="16">
          <el-col :span="8">
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
        <el-table-column prop="execution_id" label="执行ID" width="200" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="execution_time" label="执行时间(ms)" width="150" />
        <el-table-column prop="started_at" label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.started_at) }}
          </template>
        </el-table-column>
        <el-table-column prop="completed_at" label="完成时间" width="180">
          <template #default="{ row }">
            {{ row.completed_at ? formatDate(row.completed_at) : '-' }}
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
    >
      <div v-if="executionDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="执行ID">{{ executionDetail.execution_id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(executionDetail.status)" size="small">
              {{ getStatusText(executionDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="执行时间">{{ executionDetail.execution_time }}ms</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDate(executionDetail.started_at) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间" :span="2">
            {{ executionDetail.completed_at ? formatDate(executionDetail.completed_at) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="输入参数" :span="2">
            <pre>{{ JSON.stringify(executionDetail.input, null, 2) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="输出结果" :span="2">
            <pre>{{ JSON.stringify(executionDetail.output, null, 2) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="executionDetail.error_message" label="错误信息" :span="2">
            <el-alert type="error" :closable="false">{{ executionDetail.error_message }}</el-alert>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  getWorkflowList,
  deleteWorkflow,
  updateWorkflow,
  executeWorkflow as executeWorkflowAPI,
  getWorkflowExecutions,
  getExecution
} from '@/api/workflow'
import type { Workflow, WorkflowExecution } from '@/api/workflow'
import { formatDate, getWorkflowStatusType, getWorkflowStatusText } from '@/utils/formatters'

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
  inputJson: '{}'
})
const executing = ref(false)
const currentWorkflow = ref<Workflow | null>(null)

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
    workflows.value = response.items || []
    total.value = response.total || 0
    
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
  router.push('/workflow-editor')
}

// 编辑工作流
const editWorkflow = (workflow: Workflow) => {
  router.push(`/workflow-editor/${workflow.uuid}`)
}

// 执行工作流
const executeWorkflow = (workflow: Workflow) => {
  currentWorkflow.value = workflow
  executeForm.value.inputJson = '{}'
  executeDialogVisible.value = true
}

// 确认执行
const confirmExecute = async () => {
  if (!currentWorkflow.value) return
  
  try {
    const input = JSON.parse(executeForm.value.inputJson)
    executing.value = true
    
    const response = await executeWorkflowAPI(currentWorkflow.value.uuid!, {
      input
    })
    
    ElMessage.success('工作流执行已提交')
    executeDialogVisible.value = false
    
    // 跳转到执行详情
    if (response.execution_id) {
      viewExecutionDetail({ execution_id: response.execution_id } as WorkflowExecution)
    }
  } catch (error: any) {
    if (error.message?.includes('JSON')) {
      ElMessage.error('输入参数格式错误，请输入有效的JSON')
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
    const params: any = {
      workflow_uuid: currentWorkflowForExecutions.value.uuid,
      page: executionCurrentPage.value,
      pageSize: executionPageSize.value
    }
    if (executionFilterStatus.value) {
      params.status = executionFilterStatus.value
    }
    
    const response = await getWorkflowExecutions(params)
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
    const response = await getExecution(execution.execution_id)
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
    if (index !== -1) {
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

// 使用公共工具函数
const getStatusType = getWorkflowStatusType
const getStatusText = getWorkflowStatusText

onMounted(() => {
  loadWorkflows()
})
</script>

<style scoped>
.workflow-list-container {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: #f8fafc;
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
</style>

