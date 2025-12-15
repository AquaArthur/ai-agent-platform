<template>
  <div class="workflow-page-container">
    <div class="workflow-editor-wrapper">
      <!-- 顶部工具栏 -->
      <div class="top-toolbar">
        <!-- 第一行：基本操作 -->
        <div class="toolbar-row toolbar-main">
          <div class="toolbar-left">
            <el-button @click="handleGoBack" :icon="ArrowLeft" round>返回</el-button>
            <el-button @click="handleUndo" :icon="RefreshLeft" round :disabled="!canUndo" title="撤销 (Ctrl+Z)">撤销</el-button>
            <el-button @click="handleRedo" :icon="RefreshRight" round :disabled="!canRedo" title="重做 (Ctrl+Y)">重做</el-button>
            <el-button @click="autoLayout" :icon="MagicStick" round>自动排列</el-button>
            <el-button @click="fitViewNodes" :icon="FullScreen" round>居中显示</el-button>
            <el-button @click="handleValidate" :icon="CircleCheck" round :loading="validating">验证工作流</el-button>
          </div>
          <div class="toolbar-right">
            <el-button type="success" @click="handleRun" :icon="VideoPlay" round :loading="running" :disabled="!workflowUuid">
              运行
            </el-button>
            <el-button type="primary" @click="handleSave" :icon="Check" round :loading="saving">
              保存
            </el-button>
          </div>
        </div>

        <!-- 第二行：节点工具栏 -->
        <div class="toolbar-row toolbar-nodes">
          <div class="node-toolbar">
            <span class="toolbar-label">
              <el-icon><Box /></el-icon>
              节点工具箱:
            </span>
            <div class="node-buttons">
              <div
                v-for="nodeType in draggableNodeTypes"
                :key="nodeType.type"
                draggable="true"
                @dragstart="onDragStart($event, nodeType)"
                class="node-add-btn"
              >
                <div class="btn-content" :style="{ borderLeftColor: nodeType.color }">
                  <span>{{ nodeType.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- 画布区域 -->
      <div class="canvas-wrapper">
        <VueFlow
          :fit-view-on-init="true"
          :node-types="nodeTypes"
          :nodes="nodes"
          :edges="edges"
          @connect="handleConnect"
          @dragover="onDragOver"
          @drop="onDrop"
          @node-double-click="handleNodeDoubleClick"
          @nodes-change="handleNodesChange"
          @edges-change="handleEdgesChange"
          class="workflow-pane"
        >
          <Background />
          <Controls />
          <MiniMap />
        </VueFlow>
      </div>
    </div>

    <!-- 节点配置对话框 -->
    <NodeConfigDialog
      v-model="configDialogVisible"
      :node="selectedNode"
      :available-nodes="workflowNodes"
      @save="handleConfigSave"
    />

    <!-- 验证结果对话框 -->
    <el-dialog
      v-model="validationDialogVisible"
      title="工作流验证结果"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="validationResult">
        <el-alert
          :type="validationResult.valid ? 'success' : 'error'"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        >
          <template #title>
            <span style="font-size: 16px; font-weight: 600">
              {{ validationResult.valid ? '验证通过' : '验证失败' }}
            </span>
          </template>
        </el-alert>
        
        <div v-if="validationResult.message" class="validation-message">
          <h4>验证信息：</h4>
          <p>{{ validationResult.message }}</p>
        </div>
      </div>
      
      <template #footer>
        <el-button type="primary" @click="validationDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- 执行结果对话框 -->
    <el-dialog
      v-model="executionDialogVisible"
      title="工作流执行结果"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="executionResult" class="execution-result">
        <!-- 状态信息 -->
        <div class="status-header">
          <div class="status-info">
            <el-tag :type="getStatusType(executionStatus)" size="large">
              {{ getStatusText(executionStatus) }}
            </el-tag>
            <span v-if="executionResult.execution_id" class="execution-id">
              执行ID: {{ executionResult.execution_id }}
            </span>
          </div>
          <div class="time-info">
            <span v-if="executionResult.started_at" class="time-item">
              开始时间: {{ formatDate(executionResult.started_at) }}
            </span>
            <span v-if="executionResult.execution_time" class="time-item">
              耗时: {{ executionResult.execution_time }}ms
            </span>
          </div>
        </div>

        <!-- 进度条（执行中时显示） -->
        <div v-if="executionStatus === 'running'" class="progress-section">
          <el-progress :percentage="progress" :status="progressStatus" />
          <div class="progress-text">执行中，请稍候...</div>
        </div>

        <!-- 错误信息 -->
        <el-alert
          v-if="executionStatus === 'failed' && executionResult.error_message"
          type="error"
          :closable="false"
          style="margin-top: 15px;"
        >
          {{ executionResult.error_message }}
        </el-alert>

        <!-- 输出结果 -->
        <div v-if="executionStatus === 'completed' && executionResult.output" class="output-section">
          <h4>输出结果</h4>
          <div class="output-content">
            <pre>{{ formatOutput(executionResult.output) }}</pre>
          </div>
        </div>

        <!-- 节点执行记录 -->
        <div v-if="executionResult.node_executions && executionResult.node_executions.length > 0" class="node-executions">
          <h4>节点执行记录</h4>
          <el-timeline>
            <el-timeline-item
              v-for="node in sortedNodeExecutions"
              :key="node.node_id"
              :type="node.status === 'success' ? 'success' : 'danger'"
              :timestamp="formatTime(node.completed_at || node.started_at)"
              placement="top"
            >
              <div class="node-exec-card">
                <div class="node-exec-header">
                  <div class="node-title">
                    <el-icon class="status-icon" :class="node.status === 'success' ? 'success' : 'failed'">
                      <CircleCheck v-if="node.status === 'success'" />
                      <CircleClose v-else />
                    </el-icon>
                    <span class="node-name">{{ getNodeLabel(node.node_id) }}</span>
                    <el-tag size="small" type="info" effect="plain">{{ node.node_type }}</el-tag>
                  </div>
                  <div class="node-meta">
                    <span class="duration">{{ node.execution_time || 0 }}ms</span>
                  </div>
                </div>
                <div v-if="node.output" class="node-output">
                  <div class="detail-label">输出</div>
                  <pre>{{ formatOutput(node.output) }}</pre>
                </div>
                <div v-if="node.error_message" class="node-error">
                  <div class="detail-label">错误信息</div>
                  <div class="error-msg">{{ node.error_message }}</div>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="executionDialogVisible = false">关闭</el-button>
        <el-button v-if="executionStatus === 'running'" type="primary" @click="stopPolling">停止轮询</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { MiniMap } from '@vue-flow/minimap';
import type { Connection, Node, Edge, NodeChange, EdgeChange } from '@vue-flow/core';
import { Position } from '@vue-flow/core';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { MagicStick, FullScreen, Box, ArrowLeft, Check, CircleCheck, VideoPlay, CircleClose, RefreshLeft, RefreshRight } from '@element-plus/icons-vue';
import { formatDate, formatTime, getWorkflowStatusType, getWorkflowStatusText } from '@/utils/formatters';

import StartNode from './nodes/StartNode.vue';
import LLMNode from './nodes/LLMNode.vue';
import HttpNode from './nodes/HttpNode.vue';
import KnowledgeNode from './nodes/KnowledgeNode.vue';
import IntentNode from './nodes/IntentNode.vue';
import StringNode from './nodes/StringNode.vue';
import EndNode from './nodes/EndNode.vue';
import NodeConfigDialog from './components/NodeConfigDialog.vue';

import { getWorkflowByUuid, createWorkflow, updateWorkflow, validateWorkflow, executeWorkflow, getExecution, type Workflow, type WorkflowNode, type WorkflowEdge, type WorkflowExecution } from '@/api/workflow';

import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import '@vue-flow/controls/dist/style.css';
import '@vue-flow/minimap/dist/style.css';

// 定义节点类型
const nodeTypes = {
  start: StartNode,
  llm: LLMNode,
  http: HttpNode,
  knowledge: KnowledgeNode,
  intent: IntentNode,
  string: StringNode,
  end: EndNode,
} as any;

// 节点类型定义（与参考代码一致的颜色）
const nodeTypesConfig = [
  { type: 'start', label: '开始', color: '#67c23a' },
  { type: 'llm', label: 'LLM调用', color: '#409eff' },
  { type: 'http', label: 'HTTP请求', color: '#e6a23c' },
  { type: 'knowledge', label: '知识库检索', color: '#909399' },
  { type: 'intent', label: '意图识别', color: '#9c27b0' },
  { type: 'string', label: '字符串处理', color: '#00bcd4' },
  { type: 'end', label: '结束', color: '#f56c6c' }
];

// 可拖拽的节点类型（排除开始和结束节点）
const draggableNodeTypes = computed(() => {
  return nodeTypesConfig.filter(t => t.type !== 'start' && t.type !== 'end');
});

// 节点颜色配置（与参考代码一致，使用纯色作为背景，保持对象格式用于节点组件）
const nodeColors = {
  start: { bg: '#67c23a', border: 'rgba(255, 255, 255, 0.3)' },
  llm: { bg: '#409eff', border: 'rgba(255, 255, 255, 0.3)' },
  http: { bg: '#e6a23c', border: 'rgba(255, 255, 255, 0.3)' },
  knowledge: { bg: '#909399', border: 'rgba(255, 255, 255, 0.3)' },
  intent: { bg: '#9c27b0', border: 'rgba(255, 255, 255, 0.3)' },
  string: { bg: '#00bcd4', border: 'rgba(255, 255, 255, 0.3)' },
  end: { bg: '#f56c6c', border: 'rgba(255, 255, 255, 0.3)' },
};

const route = useRoute();
const router = useRouter();
const { 
  onPaneReady, 
  addEdges, 
  addNodes, 
  project, 
  getSelectedNodes, 
  getSelectedEdges, 
  removeNodes, 
  removeEdges,
  getNodes,
  getEdges,
  setNodes,
  setEdges,
  vueFlowRef,
  dimensions,
  fitView
} = useVueFlow();

// 工作流数据
const workflowUuid = computed(() => route.params.uuid as string | undefined);
const nodes = ref<Node[]>([]);
const edges = ref<Edge[]>([]);
const workflowData = ref<Workflow | null>(null);
const saving = ref(false);

// 撤销/重做历史状态管理
interface HistoryState {
  nodes: Node[];
  edges: Edge[];
}
const history = ref<HistoryState[]>([]);
const historyIndex = ref(-1);
const maxHistorySize = 50; // 最大历史记录数
const isUndoRedo = ref(false); // 标记是否正在执行撤销/重做操作，避免触发历史记录保存
const isInitialized = ref(false); // 标记历史记录是否已初始化
let saveHistoryTimer: number | null = null; // 防抖定时器

// 节点配置对话框
const configDialogVisible = ref(false);
const selectedNode = ref<WorkflowNode | null>(null);

// 验证相关
const validating = ref(false);
const validationDialogVisible = ref(false);
const validationResult = ref<{ valid: boolean; message?: string } | null>(null);

// 运行相关
const running = ref(false);
const executionId = ref<string | null>(null);
const executionStatus = ref<string>('pending');
const executionResult = ref<WorkflowExecution | null>(null);
const executionDialogVisible = ref(false);
const pollTimer = ref<number | null>(null);
const pollCount = ref(0);
const baseInterval = 1000; // 初始轮询间隔1秒
const maxInterval = 10000; // 最大轮询间隔10秒

// 将 Vue Flow 的 Node[] 转换为 WorkflowNode[] 格式
const workflowNodes = computed<WorkflowNode[]>(() => {
  return nodes.value
    .filter(node => node.type) // 过滤掉没有 type 的节点
    .map(node => ({
      id: node.id,
      type: node.type as string, // 确保 type 是 string 类型
      label: node.data?.label || '',
      position: {
        x: node.position.x,
        y: node.position.y
      },
      config: node.data?.config || {}
    }));
});

let id = 0;
const getId = () => `dndnode_${id++}`;

// 初始化默认的开始和结束节点
const initDefaultNodes = () => {
  const startNode: Node = {
    id: 'start_node',
    type: 'start',
    position: { x: 150, y: 200 },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    data: {
      label: '开始',
      config: {},
      color: nodeColors.start
    }
  };

  const endNode: Node = {
    id: 'end_node',
    type: 'end',
    position: { x: 500, y: 200 },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    data: {
      label: '结束',
      config: {},
      color: nodeColors.end
    }
  };

  setNodes([startNode, endNode]);
  nodes.value = [startNode, endNode];
  
  // 初始化历史记录
  history.value = [{
    nodes: JSON.parse(JSON.stringify([startNode, endNode])),
    edges: []
  }];
  historyIndex.value = 0;
  isInitialized.value = true;
};

const onDragStart = (event: DragEvent, nodeType: { type: string; label: string; color: string }) => {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/vueflow', nodeType.type);
    event.dataTransfer.effectAllowed = 'move';
  }
};

const onDragOver = (event: DragEvent) => {
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move';
  }
};

const onDrop = (event: DragEvent) => {
  const type = event.dataTransfer?.getData('application/vueflow');
  
  // Calculate center of the pane
  const paneCenterX = dimensions.value.width / 2;
  const paneCenterY = dimensions.value.height / 2;
  const position = project({ x: paneCenterX, y: paneCenterY });

  if (type) {
    const nodeTypeConfig = nodeTypesConfig.find(nt => nt.type === type);
    const newNode: Node = {
      id: getId(),
      type,
      position,
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
      data: {
        label: nodeTypeConfig?.label || `${type} node`,
        config: {},
        color: (nodeColors as any)[type] || nodeColors.llm // Assign color based on type, default to llm color
      }
    };
    addNodes([newNode]);
    // 添加节点后保存历史
    nextTick(() => {
      saveToHistory();
    });
  }
};

onPaneReady(({ fitView }) => {
  // 如果已经有节点，则居中显示；如果是新建工作流且还没有初始化节点，则初始化
  if (nodes.value.length > 0) {
    fitView();
  } else if (!workflowUuid.value && !isInitialized.value) {
    // 新建工作流时，初始化默认的开始和结束节点
    initDefaultNodes();
    nextTick(() => {
      fitView({ duration: 300, padding: 0.2 });
    });
  } else {
    fitView();
  }
});

const handleConnect = (connection: Connection) => {
  addEdges([connection]);
  // 连接操作后保存历史
  nextTick(() => {
    saveToHistory();
  });
};

// 处理节点双击事件
const handleNodeDoubleClick = (event: any) => {
  const node = event.node;
  if (node) {
    // 转换为WorkflowNode格式
    selectedNode.value = {
      id: node.id,
      type: node.type || '',
      label: node.data?.label || '',
      position: {
        x: node.position.x,
        y: node.position.y
      },
      config: node.data?.config || {}
    };
    configDialogVisible.value = true;
  }
};

// 保存当前状态到历史记录
const saveToHistory = () => {
  if (isUndoRedo.value) return; // 如果正在执行撤销/重做，不保存历史
  
  // 如果历史记录还未初始化，先初始化（保存当前状态作为初始状态）
  if (!isInitialized.value) {
    const currentNodes = JSON.parse(JSON.stringify(getNodes.value));
    const currentEdges = JSON.parse(JSON.stringify(getEdges.value));
    history.value = [{
      nodes: currentNodes,
      edges: currentEdges
    }];
    historyIndex.value = 0;
    isInitialized.value = true;
    return; // 初始状态不保存为可撤销的历史记录
  }
  
  // 清除防抖定时器
  if (saveHistoryTimer) {
    clearTimeout(saveHistoryTimer);
  }
  
  // 使用防抖，延迟保存历史记录（避免频繁操作时保存过多历史）
  saveHistoryTimer = window.setTimeout(() => {
    const currentNodes = JSON.parse(JSON.stringify(getNodes.value));
    const currentEdges = JSON.parse(JSON.stringify(getEdges.value));
    
    // 如果当前不在历史记录的末尾，删除后面的记录（因为用户做了新操作）
    if (historyIndex.value < history.value.length - 1) {
      history.value = history.value.slice(0, historyIndex.value + 1);
    }
    
    // 添加新的历史记录
    history.value.push({
      nodes: currentNodes,
      edges: currentEdges
    });
    
    // 限制历史记录数量（保留初始状态，所以最多保存 maxHistorySize + 1 条记录）
    if (history.value.length > maxHistorySize + 1) {
      // 保留初始状态（第0项），删除最旧的操作记录
      const firstState = history.value[0];
      const remainingStates = history.value.slice(2);
      if (firstState) {
        history.value = [firstState, ...remainingStates];
        historyIndex.value = history.value.length - 1;
      }
    } else {
      historyIndex.value = history.value.length - 1;
    }
  }, 300); // 300ms 防抖延迟
};

// 撤销操作
const handleUndo = () => {
  if (!canUndo.value) return;
  
  isUndoRedo.value = true;
  historyIndex.value--;
  
  const state = history.value[historyIndex.value];
  if (state) {
    setNodes(JSON.parse(JSON.stringify(state.nodes)));
    setEdges(JSON.parse(JSON.stringify(state.edges)));
    nodes.value = JSON.parse(JSON.stringify(state.nodes));
    edges.value = JSON.parse(JSON.stringify(state.edges));
  }
  
  nextTick(() => {
    isUndoRedo.value = false;
  });
};

// 重做操作
const handleRedo = () => {
  if (!canRedo.value) return;
  
  isUndoRedo.value = true;
  historyIndex.value++;
  
  const state = history.value[historyIndex.value];
  if (state) {
    setNodes(JSON.parse(JSON.stringify(state.nodes)));
    setEdges(JSON.parse(JSON.stringify(state.edges)));
    nodes.value = JSON.parse(JSON.stringify(state.nodes));
    edges.value = JSON.parse(JSON.stringify(state.edges));
  }
  
  nextTick(() => {
    isUndoRedo.value = false;
  });
};

// 是否可以撤销（不能撤销到初始状态之前，所以 historyIndex 必须 > 0）
const canUndo = computed(() => {
  return isInitialized.value && historyIndex.value > 0;
});

// 是否可以重做
const canRedo = computed(() => {
  return historyIndex.value < history.value.length - 1;
});

// 处理节点变化
const handleNodesChange = (_changes: NodeChange[]) => {
  // Vue Flow会自动处理节点变化，这里只需要同步到本地状态
  nodes.value = getNodes.value;
  // 保存到历史记录
  saveToHistory();
};

// 处理边变化
const handleEdgesChange = (_changes: EdgeChange[]) => {
  // Vue Flow会自动处理边变化，这里只需要同步到本地状态
  edges.value = getEdges.value;
  // 保存到历史记录
  saveToHistory();
};

// 处理配置保存
const handleConfigSave = (config: any) => {
  if (!selectedNode.value) return;

  // 更新节点数据
  const nodeId = selectedNode.value.id;
  const updatedNodes = nodes.value.map(node => {
    if (node.id === nodeId) {
      return {
        ...node,
        data: {
          ...node.data,
          config: config
        }
      };
    }
    return node;
  });

  setNodes(updatedNodes);
  nodes.value = updatedNodes;
  
  // 配置保存后保存历史
  nextTick(() => {
    saveToHistory();
  });

  // 保存到后端（如果有工作流UUID）
  if (workflowUuid.value) {
    saveWorkflow();
  }
};

// 保存工作流
const saveWorkflow = async () => {
  saving.value = true;
  try {
    const workflowNodes: WorkflowNode[] = nodes.value.map(node => ({
      id: node.id,
      type: node.type || '',
      label: node.data?.label || '',
      position: {
        x: Math.round(node.position.x),
        y: Math.round(node.position.y)
      },
      config: node.data?.config || {},
      sourcePosition: Position.Right, // Set source handle to right
      targetPosition: Position.Left, // Set target handle to left
    }));

    const workflowEdges: WorkflowEdge[] = edges.value.map(edge => ({
      id: edge.id,
      source: edge.source,
      target: edge.target
    }));

    const workflow: Workflow = {
      ...workflowData.value,
      name: workflowData.value?.name || '未命名工作流',
      nodes: workflowNodes,
      edges: workflowEdges
    };

    if (workflowUuid.value) {
      // 更新现有工作流
      await updateWorkflow(workflowUuid.value, workflow);
      ElMessage.success('工作流保存成功');
    } else {
      // 创建新工作流
      const createdWorkflow = await createWorkflow(workflow);
      workflowData.value = createdWorkflow;
      // 更新路由参数
      if (createdWorkflow.uuid) {
        router.replace(`/workflow-editor/${createdWorkflow.uuid}`);
      }
      ElMessage.success('工作流创建成功');
    }
  } catch (error: any) {
    console.error('保存工作流失败:', error);
    ElMessage.error(error.message || '保存工作流失败');
  } finally {
    saving.value = false;
  }
};

// 处理保存按钮点击
const handleSave = () => {
  saveWorkflow();
};

// 处理返回按钮点击
const handleGoBack = () => {
  router.push('/workflows');
};

// 处理验证按钮点击
const handleValidate = async () => {
  if (!workflowUuid.value) {
    ElMessage.warning('工作流尚未保存，请先保存工作流');
    return;
  }

  validating.value = true;
  try {
    const result = await validateWorkflow(workflowUuid.value);
    validationResult.value = result;
    validationDialogVisible.value = true;
    
    if (result.valid) {
      ElMessage.success('工作流验证通过');
      // 更新工作流数据中的isValid字段
      if (workflowData.value) {
        workflowData.value.isValid = true;
      }
    } else {
      ElMessage.warning('工作流验证失败：' + (result.message || '未知错误'));
      // 更新工作流数据中的isValid字段
      if (workflowData.value) {
        workflowData.value.isValid = false;
      }
    }
  } catch (error: any) {
    console.error('验证工作流失败:', error);
    ElMessage.error(error.message || '验证工作流失败');
    validationResult.value = {
      valid: false,
      message: error.message || '验证工作流失败'
    };
    validationDialogVisible.value = true;
  } finally {
    validating.value = false;
  }
};

// 加载工作流
const loadWorkflow = async () => {
  if (!workflowUuid.value) return;

  try {
    const workflow = await getWorkflowByUuid(workflowUuid.value);
    workflowData.value = workflow;

    // 转换节点数据
    let vueFlowNodes: Node[] = [];
    if (workflow.nodes && workflow.nodes.length > 0) {
      vueFlowNodes = workflow.nodes.map(node => ({
        id: node.id,
        type: node.type,
        position: node.position,
        sourcePosition: Position.Right, // Set source handle to right
        targetPosition: Position.Left, // Set target handle to left
        data: {
          label: node.label,
          config: node.config || {},
          color: (nodeColors as any)[node.type || 'llm'] || nodeColors.llm // Assign color based on type, default to llm color
        }
      }));
    }
    
    // 检查是否有开始和结束节点，如果没有则添加
    const hasStartNode = vueFlowNodes.some(node => node.type === 'start');
    const hasEndNode = vueFlowNodes.some(node => node.type === 'end');
    
    if (!hasStartNode) {
      const startNode: Node = {
        id: 'start_node',
        type: 'start',
        position: { x: 150, y: 200 },
        sourcePosition: Position.Right,
        targetPosition: Position.Left,
        data: {
          label: '开始',
          config: {},
          color: nodeColors.start
        }
      };
      vueFlowNodes.push(startNode);
    }
    
    if (!hasEndNode) {
      const endNode: Node = {
        id: 'end_node',
        type: 'end',
        position: { x: 500, y: 200 },
        sourcePosition: Position.Right,
        targetPosition: Position.Left,
        data: {
          label: '结束',
          config: {},
          color: nodeColors.end
        }
      };
      vueFlowNodes.push(endNode);
    }
    
    setNodes(vueFlowNodes);
    nodes.value = vueFlowNodes;

    // 转换边数据
    if (workflow.edges && workflow.edges.length > 0) {
      const vueFlowEdges: Edge[] = workflow.edges.map(edge => ({
        id: edge.id,
        source: edge.source,
        target: edge.target
      }));
      setEdges(vueFlowEdges);
      edges.value = vueFlowEdges;
    }

    // 重置历史记录状态（不立即初始化，等用户第一次操作时再初始化）
    history.value = [];
    historyIndex.value = -1;
    isInitialized.value = false;

    // 加载完成后自动排列节点
    await nextTick();
    if (nodes.value.length > 0) {
      autoLayout(false); // 加载时自动排列，不显示消息
    }
  } catch (error: any) {
    console.error('加载工作流失败:', error);
    ElMessage.error(error.message || '加载工作流失败');
  }
};

onMounted(() => {
  window.addEventListener('keydown', onKeyDown);
  
  // 如果有工作流UUID，加载工作流数据
  // 注意：新建工作流的节点初始化在 onPaneReady 中处理，确保画布已准备好
  if (workflowUuid.value) {
    loadWorkflow();
  }
});


const onKeyDown = (event: KeyboardEvent) => {
  // Ctrl+Z 或 Cmd+Z 撤销
  if ((event.ctrlKey || event.metaKey) && event.key === 'z' && !event.shiftKey) {
    event.preventDefault();
    handleUndo();
    return;
  }
  
  // Ctrl+Y 或 Cmd+Y 或 Ctrl+Shift+Z 重做
  if (((event.ctrlKey || event.metaKey) && event.key === 'y') || 
      ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key === 'z')) {
    event.preventDefault();
    handleRedo();
    return;
  }
  
  if (event.key === 'Delete') {
    const selectedNodes = getSelectedNodes.value;
    const selectedEdges = getSelectedEdges.value;

    if (selectedNodes.length > 0) {
      // 过滤掉开始和结束节点，不允许删除
      const nodesToRemove = selectedNodes.filter(node => 
        node.type !== 'start' && node.type !== 'end'
      );
      
      if (nodesToRemove.length < selectedNodes.length) {
        ElMessage.warning('开始和结束节点不能被删除');
      }
      
      if (nodesToRemove.length > 0) {
        removeNodes(nodesToRemove);
        // 删除节点后保存历史
        nextTick(() => {
          saveToHistory();
        });
      }
    }

    if (selectedEdges.length > 0) {
      removeEdges(selectedEdges);
      // 删除边后保存历史
      nextTick(() => {
        saveToHistory();
      });
    }
  }
  
  // Ctrl+S 或 Cmd+S 保存工作流
  if ((event.ctrlKey || event.metaKey) && event.key === 's') {
    event.preventDefault();
    if (workflowUuid.value) {
      saveWorkflow();
    }
  }
};

// 自动排列
const autoLayout = (showMessage = true) => {
  if (nodes.value.length === 0) {
    ElMessage.info('画布为空');
    return;
  }

  const startNodes = nodes.value.filter(n => n.type === 'start');
  const endNodes = nodes.value.filter(n => n.type === 'end');
  
  if (startNodes.length === 0) {
    ElMessage.warning('请先添加开始节点');
    return;
  }

  const nodeMap = new Map(nodes.value.map(n => [n.id, n]));
  const edgeMap = new Map<string, string[]>(); // source -> targets
  const reverseEdgeMap = new Map<string, string[]>(); // target -> sources

  // 构建边的映射
  edges.value.forEach(e => {
    if (!edgeMap.has(e.source)) edgeMap.set(e.source, []);
    edgeMap.get(e.source)!.push(e.target);
    
    if (!reverseEdgeMap.has(e.target)) reverseEdgeMap.set(e.target, []);
    reverseEdgeMap.get(e.target)!.push(e.source);
  });

  // 计算每个节点的层级（从开始节点开始）
  const nodeLayer = new Map();
  const visited = new Set();
  
  // 第一层：开始节点（固定在最左边）
  startNodes.forEach(n => {
    nodeLayer.set(n.id, 0);
    visited.add(n.id);
  });

  // BFS 计算其他节点的层级
  let queue = startNodes.map(n => n.id);
  
  while (queue.length > 0) {
    const nodeId = queue.shift()!;
    const currentLayer = nodeLayer.get(nodeId);
    const neighbors = edgeMap.get(nodeId) || [];
    
    neighbors.forEach((neighborId: string) => {
      const newLayer = currentLayer + 1;
      // 更新层级（取最大值，确保节点在所有前驱节点之后）
      if (!nodeLayer.has(neighborId) || nodeLayer.get(neighborId) < newLayer) {
        nodeLayer.set(neighborId, newLayer);
      }
      
      if (!visited.has(neighborId)) {
        visited.add(neighborId);
        queue.push(neighborId);
      }
    });
  }

  // 处理结束节点：如果有前驱节点，放在前驱节点的下一层；否则放在最后一层
  if (endNodes.length > 0) {
    const maxLayer = Math.max(...Array.from(nodeLayer.values()), 0);
    endNodes.forEach(endNode => {
      // 查找连接到结束节点的节点
      const predecessors = reverseEdgeMap.get(endNode.id) || [];
      
      if (predecessors.length > 0) {
        // 如果有前驱节点，计算前驱节点的最大层级，结束节点放在下一层
        let maxPredecessorLayer = -1;
        predecessors.forEach((predId: string) => {
          const predLayer = nodeLayer.get(predId);
          if (predLayer !== undefined && predLayer > maxPredecessorLayer) {
            maxPredecessorLayer = predLayer;
          }
        });
        
        if (maxPredecessorLayer >= 0) {
          // 结束节点放在前驱节点的下一层，而不是强制放在最后一层
          nodeLayer.set(endNode.id, maxPredecessorLayer + 1);
        } else {
          // 如果前驱节点没有层级（不应该发生），放在最后一层
          nodeLayer.set(endNode.id, maxLayer);
        }
      } else {
        // 如果没有前驱节点，放在最后一层（但不超过maxLayer，避免多出一层）
        if (!nodeLayer.has(endNode.id)) {
          nodeLayer.set(endNode.id, maxLayer);
        }
      }
    });
  }

  // 按层级分组节点
  const layers: string[][] = [];
  nodeLayer.forEach((layer, nodeId) => {
    if (!layers[layer]) layers[layer] = [];
    layers[layer].push(nodeId);
  });

  // 布局节点
  const layerWidth = 220; // 层间距（减小以适应更小的节点）
  const nodeHeight = 90; // 节点间距（减小以适应更小的节点）
  const startX = 150; // 起始X坐标
  const startY = 200; // 起始Y坐标

  const updatedNodes = nodes.value.map(node => {
    if (nodeLayer.has(node.id)) {
      const layerIndex = nodeLayer.get(node.id);
      const layerNodes = layers[layerIndex] || [];
      const nodeIndex = layerNodes.indexOf(node.id);
      
      // 计算这一层的总高度
      const totalHeight = (layerNodes.length - 1) * nodeHeight;
      // 计算起始Y坐标使整层垂直居中
      const layerStartY = startY - totalHeight / 2;
      
      // 如果是结束节点，减小与前驱节点的间距
      let actualX = startX + layerIndex * layerWidth;
      if (node.type === 'end') {
        const predecessors = reverseEdgeMap.get(node.id) || [];
        if (predecessors.length > 0) {
          // 查找前驱节点的最大层级
          let maxPredLayer = -1;
          predecessors.forEach((predId: string) => {
            const predLayer = nodeLayer.get(predId);
            if (predLayer !== undefined && predLayer > maxPredLayer) {
              maxPredLayer = predLayer;
            }
          });
          
          // 如果结束节点紧跟在其他节点后面，保持正常层间距
          if (maxPredLayer >= 0 && layerIndex === maxPredLayer + 1) {
            // 结束节点与前一层的间距保持为正常层间距
            actualX = startX + maxPredLayer * layerWidth + layerWidth;
          }
        }
      }
      
      return {
        ...node,
        position: {
          x: actualX,
          y: layerStartY + nodeIndex * nodeHeight
        }
      };
    }
    return node;
  });

  setNodes(updatedNodes);
  nodes.value = updatedNodes;

  nextTick(() => {
    fitViewNodes(false); // 自动排列后自动居中，不显示额外消息
    // 如果是加载时自动排列，不显示消息
    if (showMessage) {
      ElMessage.success('自动排列完成');
    }
  });
};

// 居中显示
const fitViewNodes = (showMessage = true) => {
  fitView({ duration: 300, padding: 0.2 });
  if (showMessage) {
  ElMessage.success('已居中显示');
  }
};

// 处理运行按钮点击
const handleRun = async () => {
  if (!workflowUuid.value) {
    ElMessage.warning('工作流尚未保存，请先保存工作流');
    return;
  }

  // 先保存工作流
  await saveWorkflow();

  // 检查保存是否成功
  if (!workflowUuid.value) {
    ElMessage.error('工作流保存失败，无法执行');
    return;
  }

  // 执行工作流
  running.value = true;
  executionId.value = null;
  executionStatus.value = 'pending';
  executionResult.value = null;
  pollCount.value = 0;

  try {
    // 调用执行API（这里假设不需要输入参数，如果需要可以从开始节点获取）
    const result = await executeWorkflow(workflowUuid.value, {
      input: {}
    });

    executionId.value = result.execution_id;
    executionStatus.value = result.status;
    executionResult.value = {
      execution_id: result.execution_id,
      workflow_id: workflowUuid.value,
      status: result.status,
      output: result.output,
      error_message: result.error_message,
      execution_time: result.execution_time,
      node_executions: result.node_executions || []
    };

    // 显示执行结果对话框
    executionDialogVisible.value = true;

    // 如果状态是 running 或 pending，开始轮询
    if (result.status === 'running' || result.status === 'pending') {
      startPolling();
    } else {
      // 如果已经完成或失败，停止轮询
      stopPolling();
    }

    if (result.status === 'completed') {
      ElMessage.success('工作流执行完成');
    } else if (result.status === 'failed') {
      ElMessage.error('工作流执行失败');
    }
  } catch (error: any) {
    console.error('执行工作流失败:', error);
    ElMessage.error(error.message || '执行工作流失败');
    executionStatus.value = 'failed';
    executionResult.value = {
      execution_id: '',
      workflow_id: workflowUuid.value || '',
      status: 'failed',
      error_message: error.message || '执行工作流失败',
      execution_time: 0,
      node_executions: []
    };
    executionDialogVisible.value = true;
  } finally {
    running.value = false;
  }
};

// 开始轮询
const startPolling = () => {
  if (pollTimer.value) return;
  if (!executionId.value) return;

  pollCount.value = 0;
  const poll = async () => {
    if (!executionId.value) {
      stopPolling();
      return;
    }

    pollCount.value++;
    try {
      const result = await getExecution(executionId.value);
      
      executionStatus.value = result.status;
      executionResult.value = result;

      // 如果执行完成或失败，停止轮询
      if (result.status === 'completed' || result.status === 'failed') {
        stopPolling();
        if (result.status === 'completed') {
          ElMessage.success('工作流执行完成');
        } else if (result.status === 'failed') {
          ElMessage.error('工作流执行失败');
        }
      } else {
        // 继续轮询，使用指数退避策略
        const interval = Math.min(
          baseInterval * Math.pow(1.5, Math.floor(pollCount.value / 10)),
          maxInterval
        );
        pollTimer.value = window.setTimeout(poll, interval);
      }
    } catch (error: any) {
      console.error('轮询执行状态失败:', error);
      // 错误时延长轮询间隔
      stopPolling();
      setTimeout(() => {
        if (executionId.value && (executionStatus.value === 'running' || executionStatus.value === 'pending')) {
          startPolling();
        }
      }, maxInterval * 2);
    }
  };

  poll();
};

// 停止轮询
const stopPolling = () => {
  if (pollTimer.value) {
    clearTimeout(pollTimer.value);
    pollTimer.value = null;
  }
};

// 进度计算
const progress = computed(() => {
  if (executionStatus.value === 'completed') return 100;
  if (executionStatus.value === 'failed') return 0;
  if (executionStatus.value === 'running') {
    // 根据轮询次数估算进度（简单实现）
    return Math.min(30 + pollCount.value * 5, 90);
  }
  return 0;
});

const progressStatus = computed(() => {
  if (executionStatus.value === 'failed') return 'exception';
  if (executionStatus.value === 'completed') return 'success';
  return null;
});

// 使用公共工具函数
const getStatusType = getWorkflowStatusType;
const getStatusText = getWorkflowStatusText;

// 格式化输出
const formatOutput = (val: any): string => {
  if (typeof val === 'object' && val !== null) {
    // 如果对象只有一个 output 字段，直接返回 output 的值
    if (Object.keys(val).length === 1 && 'output' in val) {
      return formatOutput(val.output); // 递归处理
    }
    // 其他情况格式化为 JSON
    return JSON.stringify(val, null, 2);
  }
  return String(val);
};

// 获取节点标签
const getNodeLabel = (nodeId: string): string => {
  const node = nodes.value.find(n => n.id === nodeId);
  return node?.data?.label || nodeId;
};

// 排序节点执行记录
const sortedNodeExecutions = computed(() => {
  if (!executionResult.value?.node_executions) return [];
  // 按开始时间排序
  return [...executionResult.value.node_executions].sort((a, b) => {
    const timeA = a.started_at ? new Date(a.started_at).getTime() : 0;
    const timeB = b.started_at ? new Date(b.started_at).getTime() : 0;
    return timeA - timeB;
  });
});

// 组件卸载时停止轮询
watch(() => executionDialogVisible.value, (visible) => {
  if (!visible) {
    stopPolling();
  }
});

onUnmounted(() => {
  stopPolling();
  window.removeEventListener('keydown', onKeyDown);
  // 清理历史记录保存定时器
  if (saveHistoryTimer) {
    clearTimeout(saveHistoryTimer);
    saveHistoryTimer = null;
  }
});

</script>

<style scoped>
.workflow-page-container {
  padding: 20px;
  min-height: calc(100vh - 64px);
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: relative;
}

.workflow-page-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 50%, rgba(102, 126, 234, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(236, 72, 153, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.workflow-editor-wrapper {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 40px);
  min-height: 600px;
  background-color: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(0, 0, 0, 0.05);
  position: relative;
  z-index: 1;
}

/* 顶部工具栏 */
.top-toolbar {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  z-index: 100;
  flex-shrink: 0;
}

.toolbar-row {
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-main {
  height: 56px;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar-nodes {
  height: 52px;
  background: #fafafa;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-toolbar {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
}

.toolbar-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.node-buttons {
  flex: 1;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.node-add-btn {
  padding: 0;
  border: none;
  background: transparent;
  transition: all 0.3s;
  cursor: move;
}

.node-add-btn .btn-content {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #fff;
  border: 1.5px solid #e4e7ed;
  border-left-width: 4px;
  border-radius: 6px;
  transition: all 0.3s;
  user-select: none;
}

.node-add-btn:hover:not(.disabled) .btn-content {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  transform: translateY(-1px);
}

.node-add-btn:active:not(.disabled) .btn-content {
  transform: scale(0.98);
}

.node-add-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.node-add-btn.disabled .btn-content {
  background: #f5f7fa;
}

.node-add-btn .btn-content span {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

/* 画布区域 */
.canvas-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.workflow-pane {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  background-image: 
    radial-gradient(circle at 20% 50%, rgba(102, 126, 234, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(236, 72, 153, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 40% 20%, rgba(16, 185, 129, 0.1) 0%, transparent 50%);
}
</style>

<style>
/* Vue Flow 全局样式 */
.workflow-pane .vue-flow__node {
  cursor: pointer;
  transition: all 0.3s ease;
  background: transparent; /* Removed !important */
  border: none; /* Removed !important */
  padding: 0 !important;
}

/* 确保节点内部组件样式不被覆盖 */
.workflow-pane .vue-flow__node > * {
  background: inherit; /* Removed !important */
}

.workflow-pane .vue-flow__node.selected {
  filter: brightness(1.1);
}

.workflow-pane .vue-flow__edge {
  cursor: pointer;
  stroke-width: 2;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.05));
}

.workflow-pane .vue-flow__edge:hover {
  stroke-width: 3;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.workflow-pane .vue-flow__edge.selected {
  stroke-width: 3;
  filter: drop-shadow(0 2px 4px rgba(102, 126, 234, 0.4));
}

.workflow-pane .vue-flow__edge-path {
  stroke: #94a3b8; /* Softer blue-gray for edges */
}

.workflow-pane .vue-flow__handle {
  width: 12px;
  height: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: 3px solid #ffffff;
  box-shadow: 
    0 2px 8px rgba(102, 126, 234, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: crosshair;
}

.workflow-pane .vue-flow__handle:hover {
  width: 14px;
  height: 14px;
  box-shadow: 
    0 4px 12px rgba(102, 126, 234, 0.6),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  transform: scale(1.2);
}

.workflow-pane .vue-flow__handle.connecting {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  box-shadow: 
    0 0 0 4px rgba(16, 185, 129, 0.2),
    0 4px 12px rgba(16, 185, 129, 0.6);
}

.workflow-pane .vue-flow__handle.connectable {
  cursor: crosshair;
}

.workflow-pane .vue-flow__connectionline {
  stroke: #94a3b8; /* Softer blue-gray for connection line */
  stroke-width: 2.5;
  stroke-dasharray: 5, 5;
  animation: dash 0.5s linear infinite;
  filter: drop-shadow(0 2px 4px rgba(148, 163, 184, 0.3));
}

@keyframes dash {
  to {
    stroke-dashoffset: -10;
  }
}

/* 选中节点的动画效果 */
.workflow-pane .vue-flow__node.selected {
  animation: nodePulse 2s ease-in-out infinite;
}

@keyframes nodePulse {
  0%, 100% {
    filter: brightness(1);
  }
  50% {
    filter: brightness(1.15);
  }
}

/* 背景网格样式优化 */
.workflow-pane .vue-flow__background {
  opacity: 0.3;
}

/* 控制按钮样式 */
.workflow-pane .vue-flow__controls {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.workflow-pane .vue-flow__controls-button {
  background: transparent;
  border: none;
  color: #667eea;
  transition: all 0.2s ease;
}

.workflow-pane .vue-flow__controls-button:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #764ba2;
}

/* 小地图样式 */
.workflow-pane .vue-flow__minimap {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* 验证结果对话框样式 */
.validation-message {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 16px;
}

.validation-message h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.validation-message p {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 执行结果对话框样式 */
.execution-result {
  padding: 10px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.execution-id {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

.time-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.time-item {
  font-size: 12px;
  color: #606266;
}

.progress-section {
  margin-top: 20px;
  margin-bottom: 20px;
}

.progress-text {
  margin-top: 10px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.output-section {
  margin-top: 20px;
}

.output-section h4 {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.output-content {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 400px;
  overflow-y: auto;
  font-size: 12px;
}

.output-content pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-word;
}

.node-executions {
  margin-top: 20px;
}

.node-executions h4 {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.node-exec-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fff;
  margin-bottom: 8px;
}

.node-exec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.node-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.node-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.duration {
  font-size: 12px;
  color: #909399;
  min-width: 45px;
  text-align: right;
}

.status-icon {
  font-size: 16px;
}

.status-icon.success {
  color: #67c23a;
}

.status-icon.failed {
  color: #f56c6c;
}

.node-output {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.node-error {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.detail-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  font-weight: 500;
}

.node-output pre {
  background: #fafafa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  margin: 0;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.error-msg {
  color: #f56c6c;
  background: #fef0f0;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
}
</style>
