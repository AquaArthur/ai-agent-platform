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
 * 通用映射函数
 */
const createMapper = <T extends string>(
  map: Record<string, T>,
  defaultValue: T
) => (key: string): T => map[key] || defaultValue

/**
 * 工作流执行状态映射
 */
const workflowStatusMap = {
  type: {
    pending: 'info',
    running: 'warning',
    completed: 'success',
    failed: 'danger',
    success: 'success'
  } as Record<string, string>,
  text: {
    pending: '等待中',
    running: '执行中',
    completed: '已完成',
    failed: '失败',
    success: '成功'
  } as Record<string, string>
}

export const getWorkflowStatusType = createMapper(workflowStatusMap.type, 'info')
export const getWorkflowStatusText = (status: string): string => 
  workflowStatusMap.text[status] || status

/**
 * 文档状态映射
 */
const documentStatusMap = {
  type: {
    uploading: 'info',
    processing: 'warning',
    processed: 'success',
    failed: 'danger'
  } as Record<string, string>,
  label: {
    uploading: '上传中',
    processing: '处理中',
    processed: '已完成',
    failed: '失败'
  } as Record<string, string>
}

export const getDocumentStatusType = createMapper(documentStatusMap.type, 'info')
export const getDocumentStatusLabel = (status: string): string => 
  documentStatusMap.label[status] || status

/**
 * 知识库作用域映射
 */
const scopeMap = {
  tagType: {
    system: 'danger',
    school: 'warning',
    course: 'success',
    agent: 'info',
    personal: ''
  } as Record<string, string>,
  label: {
    system: '系统',
    school: '学校',
    course: '课程',
    agent: '智能体',
    personal: '个人'
  } as Record<string, string>
}

export const getScopeTagType = (scopeType: string): string => 
  scopeMap.tagType[scopeType] || ''
export const getScopeLabel = (scopeType: string): string => 
  scopeMap.label[scopeType] || scopeType

/**
 * 知识库访问级别映射
 */
const accessLevelMap = {
  tagType: {
    public: 'success',
    protected: 'warning',
    private: 'info'
  } as Record<string, string>,
  label: {
    public: '公开',
    protected: '受保护',
    private: '私有'
  } as Record<string, string>
}

export const getAccessLevelTagType = (accessLevel: string): string => 
  accessLevelMap.tagType[accessLevel] || ''
export const getAccessLevelLabel = (accessLevel: string): string => 
  accessLevelMap.label[accessLevel] || accessLevel

