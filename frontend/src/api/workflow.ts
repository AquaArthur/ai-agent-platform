import { http } from '@/utils/http'

// 工作流节点类型
export interface WorkflowNode {
  id: string
  type: string
  label: string
  position: {
    x: number
    y: number
  }
  data?: any
  config?: any
}

// 工作流边类型
export interface WorkflowEdge {
  id: string
  source: string
  target: string
  label?: string
  condition?: {
    type: string
    field?: string
    value?: string
  }
}

// 工作流类型
export interface Workflow {
  id?: string
  uuid?: string
  agentId?: string
  name: string
  description?: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
  config?: Record<string, any>
  isValid?: boolean
  isActive?: boolean
  isPublic?: boolean
  executionCount?: number
  successCount?: number
  userId?: string
  createTime?: string
  updateTime?: string
}

// 工作流列表响应
export interface WorkflowListResponse {
  total: number
  items: Workflow[]
}

// 工作流执行请求
export interface WorkflowExecutionRequest {
  input?: Record<string, any>
  llm_model_id?: string
}

// 工作流执行结果
export interface WorkflowExecutionResult {
  execution_id: string
  status: string
  message?: string
  output?: any
  error_message?: string
  execution_time?: number
  node_executions?: any[]
}

// 工作流执行记录
export interface WorkflowExecution {
  id?: string
  execution_id: string
  workflow_id: string
  workflow_uuid?: string
  status: string
  input?: any
  output?: any
  error_message?: string
  execution_time?: number
  started_at?: string
  completed_at?: string
  node_executions?: any[]
}

/**
 * 查询工作流列表
 */
export const getWorkflowList = async (params?: {
  page?: number
  pageSize?: number
  search?: string
}): Promise<WorkflowListResponse> => {
  return http.get<WorkflowListResponse>('/v1/workflows', { params })
}

/**
 * 根据UUID查询工作流详情
 */
export const getWorkflowByUuid = async (uuid: string): Promise<Workflow> => {
  return http.get<Workflow>(`/v1/workflows/${uuid}`)
}

/**
 * 创建工作流
 */
export const createWorkflow = async (workflow: Workflow): Promise<Workflow> => {
  return http.post<Workflow>('/v1/workflows', workflow)
}

/**
 * 更新工作流
 */
export const updateWorkflow = async (uuid: string, workflow: Workflow): Promise<Workflow> => {
  return http.put<Workflow>(`/v1/workflows/${uuid}`, workflow)
}

/**
 * 删除工作流
 */
export const deleteWorkflow = async (uuid: string): Promise<void> => {
  return http.delete<void>(`/v1/workflows/${uuid}`)
}

/**
 * 验证工作流
 */
export const validateWorkflow = async (uuid: string): Promise<{ valid: boolean; message?: string }> => {
  return http.post<{ valid: boolean; message?: string }>(`/v1/workflows/${uuid}/validate`)
}

/**
 * 执行工作流
 */
export const executeWorkflow = async (
  uuid: string,
  request: WorkflowExecutionRequest
): Promise<WorkflowExecutionResult> => {
  return http.post<WorkflowExecutionResult>(`/v1/workflows/${uuid}/execute`, request)
}

/**
 * 获取执行记录详情
 */
export const getExecution = async (executionId: string): Promise<WorkflowExecution> => {
  return http.get<WorkflowExecution>(`/v1/workflows/executions/${executionId}`)
}

/**
 * 获取工作流执行历史列表
 */
export const getWorkflowExecutions = async (params?: {
  workflow_uuid?: string
  page?: number
  pageSize?: number
  status?: string
}): Promise<{ total: number; items: WorkflowExecution[] }> => {
  const { workflow_uuid, ...queryParams } = params || {}
  const url = workflow_uuid
    ? `/v1/workflows/${workflow_uuid}/executions`
    : '/v1/workflows/executions'
  return http.get<{ total: number; items: WorkflowExecution[] }>(url, { params: queryParams })
}

