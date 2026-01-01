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

// 工作流类型（与后端Workflow实体对齐）
export interface Workflow {
  id?: string         // 数据库主键ID
  uuid?: string       // 工作流UUID（用于外部接口）
  agentId?: string    // 所属智能体ID
  name: string
  description?: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
  config?: Record<string, any>
  isValid?: boolean   // DAG校验是否通过
  isActive?: boolean  // 工作流是否激活
  isPublic?: boolean  // 工作流是否公开
  executionCount?: number
  successCount?: number
  userId?: string     // 创建者ID
  createTime?: string
  updateTime?: string
}

// 工作流列表响应（与后端API对齐）
export interface WorkflowListResponse {
  total: number
  items: Workflow[]
}

// 工作流执行请求
export interface WorkflowExecutionRequest {
  input?: Record<string, any>
  llm_model_id?: string
}

// 工作流执行结果（与后端API对齐）
export interface WorkflowExecutionResult {
  execution_id: string
  status: string
  message?: string
}

/**
 * 工作流执行记录
 * 同时支持 camelCase 和 snake_case 以兼容后端返回格式
 */
export interface WorkflowExecution {
  id?: number
  executionId?: string
  execution_id?: string
  workflowId?: string
  workflow_id?: string
  workflowUuid?: string
  userId?: string
  user_id?: string
  status: string
  input?: Record<string, any>
  output?: Record<string, any>
  errorMessage?: string
  error_message?: string
  executionTime?: number
  execution_time?: number
  startedAt?: string
  started_at?: string
  completedAt?: string
  completed_at?: string
  nodeExecutions?: any[]
  node_executions?: any[]
  runType?: string
  run_type?: string
  createTime?: string
  create_time?: string
  updateTime?: string
  update_time?: string
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
 * 工作流验证结果
 * 同时支持 camelCase 和 snake_case 以兼容后端返回格式
 */
export interface WorkflowValidationResult {
  valid: boolean
  errorMessage?: string
  error_message?: string
  warnings?: string[]
  validationDetails?: {
    hasStartNode?: boolean
    hasEndNode?: boolean
    hasCycle?: boolean
    unreachableNodes?: string[]
  }
  validation_details?: {
    has_start_node?: boolean
    has_end_node?: boolean
    has_cycle?: boolean
    unreachable_nodes?: string[]
  }
}

/**
 * 验证工作流
 */
export const validateWorkflow = async (uuid: string): Promise<WorkflowValidationResult> => {
  return http.post<WorkflowValidationResult>(`/v1/workflows/${uuid}/validate`)
}

/**
 * 执行工作流
 * @param agentId 智能体ID（必填）
 * @param workflowId 工作流的数据库ID（必填，不是uuid）
 * @param request 执行请求，包含input输入参数和可选的llm_model_id
 */
export const executeWorkflow = async (
  agentId: string,
  workflowId: string,
  request: WorkflowExecutionRequest
): Promise<WorkflowExecutionResult> => {
  // 后端期望的格式：{ input: {...}, llm_model_id: "..." }
  const payload = {
    input: request.input || {},
    llm_model_id: request.llm_model_id
  }
  return http.post<WorkflowExecutionResult>('/v1/workflows/execute', payload, {
    params: { agentId, workflowId }
  })
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
export const getWorkflowExecutions = async (
  uuid: string,
  params?: {
    page?: number
    pageSize?: number
    status?: string
  }
): Promise<{ total: number; items: WorkflowExecution[]; workflow_uuid?: string; workflow_name?: string }> => {
  return http.get<{ total: number; items: WorkflowExecution[]; workflow_uuid?: string; workflow_name?: string }>(`/v1/workflows/${uuid}/executions`, { params })
}

