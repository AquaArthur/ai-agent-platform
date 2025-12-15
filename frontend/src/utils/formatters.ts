/**
 * 日期时间格式化工具函数
 */

/**
 * 格式化日期时间为完整格式 (年-月-日 时:分:秒)
 */
export const formatDateTime = (dateTime?: string | null): string => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/**
 * 格式化日期 (年-月-日)
 */
export const formatDate = (date?: string | null): string => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN')
}

/**
 * 格式化时间 (时:分:秒)
 */
export const formatTime = (dateTime?: string | null): string => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/**
 * 格式化文件大小
 */
export const formatSize = (bytes: number): string => {
  if (!bytes) return '0B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB'] as const
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  const index = Math.max(0, Math.min(i, sizes.length - 1))
  const sizeLabel = sizes[index] || 'B'
  return Math.round((bytes / Math.pow(k, index)) * 100) / 100 + sizeLabel
}

/**
 * 工作流执行状态类型映射
 */
export const getWorkflowStatusType = (status: string): string => {
  const types: Record<string, string> = {
    pending: 'info',
    running: 'warning',
    completed: 'success',
    failed: 'danger',
    success: 'success'
  }
  return types[status] || 'info'
}

/**
 * 工作流执行状态文本映射
 */
export const getWorkflowStatusText = (status: string): string => {
  const texts: Record<string, string> = {
    pending: '等待中',
    running: '执行中',
    completed: '已完成',
    failed: '失败',
    success: '成功'
  }
  return texts[status] || status
}

/**
 * 文档状态类型映射
 */
export const getDocumentStatusType = (status: string): string => {
  const types: Record<string, string> = {
    uploading: 'info',
    processing: 'warning',
    processed: 'success',
    failed: 'danger'
  }
  return types[status] || 'info'
}

/**
 * 文档状态文本映射
 */
export const getDocumentStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    uploading: '上传中',
    processing: '处理中',
    processed: '已完成',
    failed: '失败'
  }
  return labels[status] || status
}

/**
 * 知识库作用域类型映射
 */
export const getScopeTagType = (scopeType: string): string => {
  const typeMap: Record<string, string> = {
    system: 'danger',
    school: 'warning',
    course: 'success',
    agent: 'info',
    personal: ''
  }
  return typeMap[scopeType] || ''
}

/**
 * 知识库作用域标签文本映射
 */
export const getScopeLabel = (scopeType: string): string => {
  const labelMap: Record<string, string> = {
    system: '系统',
    school: '学校',
    course: '课程',
    agent: '智能体',
    personal: '个人'
  }
  return labelMap[scopeType] || scopeType
}

/**
 * 知识库访问级别类型映射
 */
export const getAccessLevelTagType = (accessLevel: string): string => {
  const typeMap: Record<string, string> = {
    public: 'success',
    protected: 'warning',
    private: 'info'
  }
  return typeMap[accessLevel] || ''
}

/**
 * 知识库访问级别标签文本映射
 */
export const getAccessLevelLabel = (accessLevel: string): string => {
  const labelMap: Record<string, string> = {
    public: '公开',
    protected: '受保护',
    private: '私有'
  }
  return labelMap[accessLevel] || accessLevel
}

