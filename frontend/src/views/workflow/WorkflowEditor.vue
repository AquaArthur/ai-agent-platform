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
        
        <div v-if="validationErrorMsg" class="validation-message">
          <h4>验证信息：</h4>
          <p>{{ validationErrorMsg }}</p>
        </div>
        
        <div v-if="validationResult.warnings && validationResult.warnings.length > 0" class="validation-warnings">
          <h4>警告信息：</h4>
          <ul>
            <li v-for="(warning, index) in validationResult.warnings" :key="index">{{ warning }}</li>
          </ul>
        </div>
        
        <div v-if="validationDetails" class="validation-details">
          <h4>验证详情：</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="有开始节点">
              {{ (getField(validationDetails, 'hasStartNode', 'has_start_node', false)) ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="有结束节点">
              {{ (getField(validationDetails, 'hasEndNode', 'has_end_node', false)) ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="存在循环依赖">
              {{ (getField(validationDetails, 'hasCycle', 'has_cycle', false)) ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="不可达节点">
              {{ (getField(validationDetails, 'unreachableNodes', 'unreachable_nodes', []) || []).length > 0 
                ? (getField(validationDetails, 'unreachableNodes', 'unreachable_nodes', []) || []).join(', ')
                : '无' }}
            </el-descriptions-item>
          </el-descriptions>
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
            <el-tag :type="getWorkflowStatusType(executionStatus)" size="large">
              {{ getWorkflowStatusText(executionStatus) }}
            </el-tag>
            <span v-if="executionId" class="execution-id">
              执行ID: {{ executionId }}
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
          v-if="executionStatus === 'failed' && executionErrorMsg"
          type="error"
          :closable="false"
          style="margin-top: 15px;"
        >
          {{ executionErrorMsg }}
        </el-alert>

        <!-- 输出结果 -->
        <div v-if="executionStatus === 'completed' && executionResult.output" class="output-section">
          <h4>输出结果</h4>
          <div class="output-content">
            <pre class="json-display">{{ formatOutput(executionResult.output) }}</pre>
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
                  <pre class="json-display">{{ formatOutput(node.output) }}</pre>
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
import { getNodeLabelById, sortNodeExecutions, getField } from '@/utils/workflow';
import { useWorkflowExecution } from '@/composables/useWorkflowExecution';

import StartNode from './nodes/StartNode.vue';
import LLMNode from './nodes/LLMNode.vue';
import HttpNode from './nodes/HttpNode.vue';
import KnowledgeNode from './nodes/KnowledgeNode.vue';
import IntentNode from './nodes/IntentNode.vue';
import StringNode from './nodes/StringNode.vue';
import EndNode from './nodes/EndNode.vue';
import NodeConfigDialog from './components/NodeConfigDialog.vue';

import { getWorkflowByUuid, createWorkflow, updateWorkflow, validateWorkflow, type Workflow, type WorkflowNode, type WorkflowEdge, type WorkflowValidationResult } from '@/api/workflow';

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

// 可拖拽的节点类型（包含所有节点类型）
const draggableNodeTypes = computed(() => nodeTypesConfig)

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
  setNodes,
  setEdges,
  dimensions,
  fitView,
  nodes,
  edges
} = useVueFlow();

// 工作流数据
const workflowUuid = computed(() => route.params.uuid as string | undefined);
const workflowData = ref<Workflow | null>(null);
const saving = ref(false);

// 自动保存相关
const autoSaveInterval = ref<number | null>(null);
const autoSaveIntervalMs = 30000; // 30秒自动保存一次
const lastSaveTime = ref<number>(0);
const hasUnsavedChanges = ref(false);

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
const validationResult = ref<WorkflowValidationResult | null>(null);

// 运行相关 - 使用组合式函数
const {
  running,
  executionStatus,
  executionResult,
  executionDialogVisible,
  progress,
  progressStatus,
  runWorkflow,
  stopPolling,
  resetExecution,
  formatOutput
} = useWorkflowExecution();

// 将 Vue Flow 的 Node[] 转换为 WorkflowNode[] 格式
const workflowNodes = computed<WorkflowNode[]>(() => {
  const currentNodes = nodes.value || [];
  return currentNodes
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
      hasUnsavedChanges.value = true;
    });
  }
};

onPaneReady(({ fitView }) => {
  // 等待一下确保状态已同步
  nextTick(() => {
    // 如果已经有节点，则居中显示；如果是新建工作流且还没有初始化节点，则初始化
    if (nodes.value && nodes.value.length > 0) {
      fitView({ duration: 300, padding: 0.2 });
    } else if (!workflowUuid.value && !isInitialized.value) {
      // 新建工作流时，初始化默认的开始和结束节点
      initDefaultNodes();
      nextTick(() => {
        fitView({ duration: 300, padding: 0.2 });
      });
    } else {
      fitView({ duration: 300, padding: 0.2 });
    }
  });
});

const handleConnect = (connection: Connection) => {
  addEdges([connection]);
  // 连接操作后保存历史
  nextTick(() => {
    saveToHistory();
    hasUnsavedChanges.value = true;
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
    const currentNodes = JSON.parse(JSON.stringify(nodes.value || []));
    const currentEdges = JSON.parse(JSON.stringify(edges.value || []));
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
    const currentNodes = JSON.parse(JSON.stringify(nodes.value || []));
    const currentEdges = JSON.parse(JSON.stringify(edges.value || []));
    
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
  // Vue Flow会自动处理节点变化，保存到历史记录
  saveToHistory();
  // 标记有未保存的更改
  hasUnsavedChanges.value = true;
};

// 处理边变化
const handleEdgesChange = (_changes: EdgeChange[]) => {
  // Vue Flow会自动处理边变化，保存到历史记录
  saveToHistory();
  // 标记有未保存的更改
  hasUnsavedChanges.value = true;
};

// 处理配置保存
const handleConfigSave = (config: any) => {
  if (!selectedNode.value) return;

  // 更新节点数据
  const nodeId = selectedNode.value.id;
  const currentNodes = nodes.value || [];
  const updatedNodes = currentNodes.map(node => {
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
  
  // 配置保存后保存历史
  nextTick(() => {
    saveToHistory();
  });

  // 标记有未保存的更改
  hasUnsavedChanges.value = true;
  
  // 保存到后端（如果有工作流UUID）
  if (workflowUuid.value) {
    saveWorkflow();
  }
};

// 保存工作流
const saveWorkflow = async (silent = false) => {
  // 如果正在保存，跳过
  if (saving.value) return;
  
  // 如果没有工作流UUID且没有未保存的更改，跳过
  if (!workflowUuid.value && !hasUnsavedChanges.value) return;
  
  saving.value = true;
  try {
    // 确保获取最新的节点和边数据
    const currentNodes = nodes.value || [];
    const currentEdges = edges.value || [];
    
    // 检查是否有节点
    if (currentNodes.length === 0) {
      if (!silent) {
        ElMessage.warning('工作流没有节点，无法保存');
      }
      return;
    }
    
    const nodesToSave: WorkflowNode[] = currentNodes
      .filter(node => node.type) // 只保存有类型的节点
      .map(node => ({
        id: node.id,
        type: node.type || '',
        label: node.data?.label || '',
        position: {
          x: Math.round(node.position.x),
          y: Math.round(node.position.y)
        },
        config: node.data?.config || {}
      }));

    const edgesToSave: WorkflowEdge[] = currentEdges.map(edge => ({
      id: edge.id,
      source: edge.source,
      target: edge.target
    }));

    const workflow: Workflow = {
      ...workflowData.value,
      name: workflowData.value?.name || '未命名工作流',
      nodes: nodesToSave,
      edges: edgesToSave
    };

    if (workflowUuid.value) {
      // 更新现有工作流
      const updatedWorkflow = await updateWorkflow(workflowUuid.value, workflow);
      workflowData.value = updatedWorkflow;
      if (!silent) {
        ElMessage.success('工作流保存成功');
      }
    } else {
      // 创建新工作流
      const createdWorkflow = await createWorkflow(workflow);
      workflowData.value = createdWorkflow;
      // 更新路由参数
      if (createdWorkflow.uuid) {
        router.replace(`/workflow-editor/${createdWorkflow.uuid}`);
      }
      if (!silent) {
        ElMessage.success('工作流创建成功');
      }
    }
    
    // 更新保存时间和未保存状态
    lastSaveTime.value = Date.now();
    hasUnsavedChanges.value = false;
  } catch (error: any) {
    console.error('保存工作流失败:', error);
    if (!silent) {
      ElMessage.error(error.message || '保存工作流失败');
    }
  } finally {
    saving.value = false;
  }
};

// 自动保存工作流
const autoSaveWorkflow = async () => {
  // 只有在有工作流UUID且有未保存的更改时才自动保存
  if (!workflowUuid.value || !hasUnsavedChanges.value || saving.value) {
    return;
  }
  
  // 检查距离上次保存的时间（如果从未保存过，lastSaveTime 为 0，需要保存）
  const timeSinceLastSave = Date.now() - (lastSaveTime.value || 0);
  if (timeSinceLastSave >= autoSaveIntervalMs) {
    await saveWorkflow(true); // 静默保存，不显示消息
  }
};

// 启动自动保存
const startAutoSave = () => {
  // 清除现有的定时器
  if (autoSaveInterval.value) {
    clearInterval(autoSaveInterval.value);
  }
  
  // 设置新的定时器
  autoSaveInterval.value = window.setInterval(() => {
    autoSaveWorkflow();
  }, autoSaveIntervalMs);
};

// 停止自动保存
const stopAutoSave = () => {
  if (autoSaveInterval.value) {
    clearInterval(autoSaveInterval.value);
    autoSaveInterval.value = null;
  }
};

// 处理保存按钮点击
const handleSave = () => {
  saveWorkflow(false); // 显示保存消息
};

// 处理返回按钮点击
const handleGoBack = () => {
  router.push('/workflows');
};

// 处理验证按钮点击
const handleValidate = async () => {
  // 验证前先保存工作流（如果没有UUID，会先创建）
  await saveWorkflow(true); // 静默保存
  
  // 保存后再次检查UUID（新建工作流保存后会获得UUID）
  if (!workflowUuid.value) {
    ElMessage.warning('工作流保存失败，无法验证');
    return;
  }

  validating.value = true;
  try {
    const result = await validateWorkflow(workflowUuid.value);
    validationResult.value = result;
    validationDialogVisible.value = true;
    
    if (result.valid) {
      ElMessage.success('工作流验证通过');
      if (workflowData.value) {
        workflowData.value.isValid = true;
      }
    } else {
      const errorMsg = getField(result, 'errorMessage', 'error_message', '未知错误')
      ElMessage.warning('工作流验证失败：' + errorMsg);
      if (workflowData.value) {
        workflowData.value.isValid = false;
      }
    }
  } catch (error: any) {
    console.error('验证工作流失败:', error);
    ElMessage.error(error.message || '验证工作流失败');
    validationResult.value = {
      valid: false,
      errorMessage: error.message || '验证工作流失败'
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
      vueFlowNodes = workflow.nodes.map(node => {
        // 确保 position 存在且有效
        const position = node.position || { x: 0, y: 0 };
        return {
          id: node.id,
          type: node.type,
          position: {
            x: typeof position.x === 'number' ? position.x : parseInt(String(position.x)) || 0,
            y: typeof position.y === 'number' ? position.y : parseInt(String(position.y)) || 0
          },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
          data: {
            label: node.label || '',
            config: node.config || {},
            color: (nodeColors as any)[node.type || 'llm'] || nodeColors.llm
          }
        };
      });
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
    
    // 设置节点和边
    setNodes(vueFlowNodes);

    // 转换边数据
    const vueFlowEdges: Edge[] = [];
    if (workflow.edges && workflow.edges.length > 0) {
      workflow.edges.forEach(edge => {
        // 验证边的源节点和目标节点是否存在
        const sourceExists = vueFlowNodes.some(n => n.id === edge.source);
        const targetExists = vueFlowNodes.some(n => n.id === edge.target);
        if (sourceExists && targetExists) {
          vueFlowEdges.push({
            id: edge.id || `edge_${edge.source}_${edge.target}`,
            source: edge.source,
            target: edge.target
          });
        }
      });
    }
    setEdges(vueFlowEdges);

    // 重置历史记录状态（不立即初始化，等用户第一次操作时再初始化）
    history.value = [];
    historyIndex.value = -1;
    isInitialized.value = false;

    // 重置未保存状态和保存时间
    hasUnsavedChanges.value = false;
    lastSaveTime.value = Date.now();

    // 等待 Vue Flow 更新状态后再进行后续操作
    await nextTick();
    await nextTick(); // 双重 nextTick 确保状态已更新
    
    // 加载完成后自动排列节点
    if (vueFlowNodes.length > 0) {
      // 使用 setTimeout 确保画布已完全渲染
      setTimeout(() => {
        autoLayout(false); // 加载时自动排列，不显示消息
      }, 100);
    }
  } catch (error: any) {
    console.error('加载工作流失败:', error);
    ElMessage.error(error.message || '加载工作流失败');
  }
};

onMounted(() => {
  window.addEventListener('keydown', onKeyDown);
  
  // 启动自动保存
  startAutoSave();
  
  // 如果有工作流UUID，加载工作流数据
  // 注意：新建工作流的节点初始化在 onPaneReady 中处理，确保画布已准备好
  if (workflowUuid.value) {
    loadWorkflow();
  } else {
    // 新建工作流时，初始化保存时间
    lastSaveTime.value = Date.now();
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
          hasUnsavedChanges.value = true;
        });
      }
    }

    if (selectedEdges.length > 0) {
      removeEdges(selectedEdges);
      // 删除边后保存历史
      nextTick(() => {
        saveToHistory();
        hasUnsavedChanges.value = true;
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
const autoLayout = async (showMessage = true) => {
  // 用户手动点击自动排列时，先保存工作流
  if (showMessage && workflowUuid.value) {
    await saveWorkflow(true); // 静默保存
  }
  
  const currentNodes = nodes.value || [];
  const currentEdges = edges.value || [];
  
  if (currentNodes.length === 0) {
    if (showMessage) {
      ElMessage.info('画布为空');
    }
    return;
  }

  const startNodes = currentNodes.filter(n => n.type === 'start');
  const endNodes = currentNodes.filter(n => n.type === 'end');
  
  if (startNodes.length === 0) {
    if (showMessage) {
      ElMessage.warning('请先添加开始节点');
    }
    return;
  }

  const edgeMap = new Map<string, string[]>(); // source -> targets
  const reverseEdgeMap = new Map<string, string[]>(); // target -> sources

  // 构建边的映射
  currentEdges.forEach(e => {
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

  // 处理孤立节点（没有连接到开始节点的节点）
  const isolatedNodes = currentNodes.filter(node => !nodeLayer.has(node.id));
  if (isolatedNodes.length > 0) {
    // 计算最大层级，孤立节点放在右侧
    const maxLayer = nodeLayer.size > 0 ? Math.max(...Array.from(nodeLayer.values()), 0) : -1;
    const isolatedLayer = maxLayer + 1;
    
    // 为孤立节点分配层级
    isolatedNodes.forEach((node) => {
      nodeLayer.set(node.id, isolatedLayer);
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

  // 特殊处理：如果只有开始和结束节点，让它们水平排列（左右排列）
  const onlyStartAndEnd = currentNodes.length === 2 && 
    startNodes.length === 1 && 
    endNodes.length === 1 &&
    currentNodes.every(n => n.type === 'start' || n.type === 'end');

  const updatedNodes = currentNodes.map(node => {
    if (nodeLayer.has(node.id)) {
      const layerIndex = nodeLayer.get(node.id);
      const layerNodes = layers[layerIndex] || [];
      const nodeIndex = layerNodes.indexOf(node.id);
      
      // 如果只有开始和结束节点，让它们水平排列
      if (onlyStartAndEnd) {
        let actualX = startX;
        if (node.type === 'end') {
          actualX = startX + layerWidth;
        }
        return {
          ...node,
          position: {
            x: actualX,
            y: startY // 同一Y坐标，水平排列
          }
        };
      }
      
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
    // 如果节点仍然不在 nodeLayer 中（不应该发生，但为了安全起见）
    return node;
  });

  setNodes(updatedNodes);
  
  // 标记有未保存的更改
  hasUnsavedChanges.value = true;

  nextTick(() => {
    fitViewNodes(false); // 自动排列后自动居中，不显示额外消息
    // 如果是加载时自动排列，不显示消息
    if (showMessage) {
      ElMessage.success('自动排列完成');
    }
  });
};

// 居中显示
const fitViewNodes = async (showMessage = true) => {
  // 用户手动点击居中显示时，先保存工作流
  if (showMessage && workflowUuid.value) {
    await saveWorkflow(true); // 静默保存
  }
  
  fitView({ duration: 300, padding: 0.2 });
  if (showMessage) {
    ElMessage.success('已居中显示');
  }
};

// 直接使用导入的工具函数

// 获取节点标签
const getNodeLabel = (nodeId: string): string => {
  return getNodeLabelById(nodeId, workflowNodes.value);
};

// 排序节点执行记录
const sortedNodeExecutions = computed(() => {
  const nodeExecs = getField(executionResult.value, 'nodeExecutions', 'node_executions', []) || []
  return nodeExecs.length > 0 ? sortNodeExecutions(nodeExecs) : []
})

// 验证结果字段访问（兼容 snake_case 和 camelCase）
const validationErrorMsg = computed(() => 
  getField(validationResult.value, 'errorMessage', 'error_message', null)
)
const validationDetails = computed(() => 
  getField(validationResult.value, 'validationDetails', 'validation_details', null)
)

// 执行结果字段访问（兼容 snake_case 和 camelCase）
const executionId = computed(() => 
  getField(executionResult.value, 'executionId', 'execution_id', null)
)
const executionErrorMsg = computed(() => 
  getField(executionResult.value, 'errorMessage', 'error_message', null)
)

// 处理运行按钮点击
const handleRun = async () => {
  // 运行前先保存工作流（如果没有UUID，会先创建）
  await saveWorkflow(true); // 静默保存
  
  // 保存后再次检查UUID（新建工作流保存后会获得UUID）
  if (!workflowUuid.value) {
    ElMessage.warning('工作流保存失败，无法执行');
    return;
  }

  // 运行前先验证工作流
  try {
    const result = await validateWorkflow(workflowUuid.value);
    if (!result.valid) {
      const errorMsg = getField(result, 'errorMessage', 'error_message', '未知错误')
      ElMessage.warning('工作流验证失败，无法运行：' + errorMsg);
      // 显示验证结果对话框
      validationResult.value = result;
      validationDialogVisible.value = true;
      return;
    }
  } catch (error: any) {
    console.error('验证工作流失败:', error);
    ElMessage.error('验证工作流失败：' + (error.message || '未知错误'));
    return;
  }

  // 验证通过，执行工作流
  await runWorkflow(workflowUuid.value, { input: {} });
};

// 组件卸载时停止轮询
watch(() => executionDialogVisible.value, (visible) => {
  if (!visible) {
    stopPolling();
  }
});

onUnmounted(() => {
  resetExecution();
  stopAutoSave(); // 停止自动保存
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
  background: var(--gradient-bg-secondary);
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
  background: var(--gradient-bg-secondary);
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
  background: var(--gradient-bg-card-header);
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
  background: #fef0f0;
  border-radius: 8px;
  margin-top: 16px;
  border-left: 4px solid #f56c6c;
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

.validation-warnings {
  padding: 16px;
  background: #fdf6ec;
  border-radius: 8px;
  margin-top: 16px;
  border-left: 4px solid #e6a23c;
}

.validation-warnings h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.validation-warnings ul {
  margin: 0;
  padding-left: 20px;
}

.validation-warnings li {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}

.validation-details {
  margin-top: 16px;
}

.validation-details h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
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
  padding: 0;
  border-radius: 4px;
  max-height: 400px;
  overflow-y: auto;
  font-size: 12px;
}

.json-display {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin: 0;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre;
  overflow-x: auto;
  overflow-y: visible;
  max-width: 100%;
  width: 100%;
  box-sizing: border-box;
  display: block;
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

.node-output .json-display {
  background: #fafafa;
  padding: 8px;
}

.error-msg {
  color: #f56c6c;
  background: #fef0f0;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
}
</style>
