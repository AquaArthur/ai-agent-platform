/**
 * 工作流相关工具函数
 */
import type { Workflow, WorkflowNode, WorkflowExecution } from '@/api/workflow'

/**
 * 检查工作流是否需要LLM模型
 */
export const checkIfNeedsLlmModel = (workflow: Workflow): boolean => {
  if (!workflow.nodes || workflow.nodes.length === 0) {
    return false
  }
  
  // 检查是否包含LLM节点或意图识别节点
  return workflow.nodes.some(node => {
    const nodeType = node.type || ''
    return nodeType === 'llm' || nodeType === 'intent'
  })
}

/**
 * 格式化输出结果
 */
export const formatWorkflowOutput = (val: any): string => {
  if (typeof val === 'object' && val !== null) {
    // 如果对象只有一个 output 字段，直接返回 output 的值
    if (Object.keys(val).length === 1 && 'output' in val) {
      return formatWorkflowOutput(val.output) // 递归处理
    }
    // 其他情况格式化为 JSON
    return JSON.stringify(val, null, 2)
  }
  return String(val)
}

/**
 * 根据节点ID获取节点标签
 */
export const getNodeLabelById = (nodeId: string, nodes: WorkflowNode[]): string => {
  const node = nodes.find(n => n.id === nodeId)
  return node?.label || nodeId
}

/**
 * 工作流默认输入配置
 */
export const workflowInputConfigs = [
  {
    keywords: ['智能家居', 'home-ctrl', 'wf-001'],
    input: { user_message: '打开客厅的灯' },
    tip: '请输入用户消息，例如：{"user_message": "打开客厅的灯"}'
  },
  {
    keywords: ['定时关闭', 'auto-off', 'wf-002'],
    input: { trigger: 'scheduled' },
    tip: '请输入触发信息，例如：{"trigger": "scheduled"}'
  },
  {
    keywords: ['足球', 'llm-only', 'wf-003', '足球冠军'],
    input: { team_name: '巴塞罗那' },
    tip: '请输入球队名称，例如：{"team_name": "巴塞罗那"}。支持：皇家马德里、曼联、拜仁慕尼黑、尤文图斯、巴黎圣日耳曼等'
  },
  {
    keywords: ['HTTP', 'http-only', 'wf-004', 'HTTP节点'],
    input: {},
    tip: '此工作流不需要输入参数，使用空对象 {} 即可'
  },
  {
    keywords: ['知识库', 'knowledge-only', 'wf-005', '知识库检索'],
    input: { question: '如何重置路由器密码？' },
    tip: '请输入查询问题，例如：{"question": "如何重置路由器密码？"}'
  },
  {
    keywords: ['意图识别', 'intent-only', 'wf-006'],
    input: { user_input: '我想了解一下产品的售后服务政策' },
    tip: '请输入用户输入文本，例如：{"user_input": "我想了解一下产品的售后服务政策"}'
  },
  {
    keywords: ['字符串', 'string-only', 'wf-007', '字符串处理'],
    input: { first_name: '张', last_name: '三' },
    tip: '请输入姓名信息，例如：{"first_name": "张", "last_name": "三"}'
  },
  {
    keywords: ['复杂字符串', 'complex-string', 'wf-008'],
    input: (): Record<string, any> => ({
      rawText: '  hello OLD world  ',
      timestamp: new Date().toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }).replace(/\//g, '-')
    }),
    tip: '请输入原始文本和时间戳，例如：{"rawText": "  hello OLD world  ", "timestamp": "2025-12-11 10:00:00"}'
  }
] as const

/**
 * 根据工作流获取默认输入JSON和提示信息
 */
export const getDefaultWorkflowInput = (workflow: Workflow): { json: string; tip: string } => {
  const name = workflow.name || ''
  const uuid = workflow.uuid || ''
  const description = workflow.description || ''
  const searchText = `${name} ${uuid} ${description}`.toLowerCase()
  
  // 查找匹配的配置
  for (const config of workflowInputConfigs) {
    if (config.keywords.some(keyword => searchText.includes(keyword.toLowerCase()))) {
      const input = typeof config.input === 'function' ? config.input() : config.input
      return {
        json: JSON.stringify(input, null, 2),
        tip: config.tip
      }
    }
  }
  
  // 默认返回空对象
  return {
    json: JSON.stringify({}, null, 2),
    tip: '请输入JSON格式的输入参数，例如：{"query": "你好"}'
  }
}

/**
 * 排序节点执行记录（按开始时间）
 */
export const sortNodeExecutions = (nodeExecutions: any[]) => {
  return [...nodeExecutions].sort((a, b) => {
    const timeA = a.started_at ? new Date(a.started_at).getTime() : 0
    const timeB = b.started_at ? new Date(b.started_at).getTime() : 0
    return timeA - timeB
  })
}

/**
 * 统一获取字段值（兼容 snake_case 和 camelCase）
 * @param obj - 对象
 * @param camelKey - camelCase 字段名
 * @param snakeKey - snake_case 字段名（可选，默认根据 camelKey 自动生成）
 * @param defaultValue - 默认值
 */
export const getField = <T = any>(
  obj: any,
  camelKey: string,
  snakeKey?: string,
  defaultValue: T | null = null
): T | null => {
  if (!obj) return defaultValue
  const snake = snakeKey || camelKey.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`)
  return obj[camelKey] ?? obj[snake] ?? defaultValue
}

