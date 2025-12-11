package org.demo.core.workflow.executor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.Workflow;
import org.demo.core.workflow.node.BaseNodeConfig;
import org.demo.core.workflow.node.NodeConfigFactory;
import org.demo.core.workflow.util.VariableResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 工作流执行引擎
 * 使用线程池管理工作流执行任务，每个任务代表一个工作流的串行执行
 */
@Slf4j
@Component
public class WorkflowExecutor {

    @Autowired
    private List<NodeExecutor> nodeExecutors;

    private Map<String, NodeExecutor> executorMap;
    private ExecutorService executorService;

    /**
     * 线程池配置
     */
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;
    private static final long KEEP_ALIVE_TIME = 60L;

    @PostConstruct
    public void init() {
        // 初始化节点执行器映射
        executorMap = new HashMap<>();
        for (NodeExecutor executor : nodeExecutors) {
            executorMap.put(executor.getSupportedType(), executor);
        }
        log.info("已注册 {} 个节点执行器", executorMap.size());

        // 初始化线程池
        executorService = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        log.info("工作流执行引擎已初始化，线程池大小: {}-{}", CORE_POOL_SIZE, MAX_POOL_SIZE);
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
                log.info("工作流执行引擎已关闭");
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 异步执行工作流
     *
     * @param workflow 工作流定义
     * @param input 输入参数
     * @param llmModelId LLM模型ID
     * @param callback 执行完成回调
     */
    public void executeAsync(Workflow workflow, Map<String, Object> input, String llmModelId, ExecutionCallback callback) {
        executorService.submit(() -> {
            try {
                WorkflowExecutionResult result = execute(workflow, input, llmModelId);
                callback.onComplete(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * 同步执行工作流
     *
     * @param workflow 工作流定义
     * @param input 输入参数
     * @param llmModelId LLM模型ID
     * @return 执行结果
     */
    public WorkflowExecutionResult execute(Workflow workflow, Map<String, Object> input, String llmModelId) {
        WorkflowExecutionResult result = new WorkflowExecutionResult();
        result.setWorkflowId(workflow.getId());
        result.setStartTime(LocalDateTime.now());
        result.setStatus("running");

        try {
            // 1. 验证工作流
            validateWorkflow(workflow);

            // 2. 初始化执行上下文
            Map<String, Object> nodeOutputs = new HashMap<>();
            ExecutionContext context = new ExecutionContext(input, nodeOutputs, llmModelId);

            // 3. 构建执行顺序
            List<Workflow.WorkflowNode> executionOrder = buildExecutionOrder(workflow);
            log.info("工作流 {} 执行顺序: {}", workflow.getId(), 
                executionOrder.stream().map(Workflow.WorkflowNode::getId).collect(Collectors.toList()));

            // 4. 串行执行节点
            List<NodeExecutionRecord> nodeRecords = new ArrayList<>();
            for (Workflow.WorkflowNode node : executionOrder) {
                NodeExecutionRecord nodeRecord = executeNode(node, context);
                nodeRecords.add(nodeRecord);

                // 如果节点执行失败且配置为失败停止，则终止工作流
                if ("failed".equals(nodeRecord.getStatus()) && isStopOnError(workflow)) {
                    result.setStatus("failed");
                    result.setErrorMessage("节点 " + node.getId() + " 执行失败: " + nodeRecord.getErrorMessage());
                    break;
                }
            }

            // 5. 设置执行结果
            result.setNodeExecutions(nodeRecords);
            result.setOutput(context.getNodeOutputs());
            
            if (!"failed".equals(result.getStatus())) {
                result.setStatus("completed");
            }

        } catch (Exception e) {
            log.error("工作流 {} 执行失败", workflow.getId(), e);
            result.setStatus("failed");
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setEndTime(LocalDateTime.now());
        }

        return result;
    }

    /**
     * 执行单个节点
     */
    private NodeExecutionRecord executeNode(Workflow.WorkflowNode node, ExecutionContext context) {
        NodeExecutionRecord record = new NodeExecutionRecord();
        record.setNodeId(node.getId());
        record.setNodeType(node.getType());
        record.setStartTime(LocalDateTime.now());
        record.setStatus("running");

        try {
            log.info("开始执行节点: {} ({})", node.getId(), node.getType());

            // 1. 创建节点配置
            BaseNodeConfig config = NodeConfigFactory.createConfig(node);

            // 2. 替换配置中的变量
            Object resolvedConfig = VariableResolver.resolveVariables(config, context);
            BaseNodeConfig finalConfig = (BaseNodeConfig) resolvedConfig;

            // 3. 获取节点执行器
            NodeExecutor executor = executorMap.get(node.getType());
            if (executor == null) {
                throw new IllegalArgumentException("不支持的节点类型: " + node.getType());
            }

            // 4. 执行节点
            Object output = executor.execute(node, finalConfig, context);

            // 5. 保存节点输出
            context.saveNodeOutput(node.getId(), output);

            record.setStatus("completed");
            record.setOutput(output);
            log.info("节点 {} 执行成功", node.getId());

        } catch (Exception e) {
            log.error("节点 {} 执行失败", node.getId(), e);
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
        } finally {
            record.setEndTime(LocalDateTime.now());
        }

        return record;
    }

    /**
     * 构建节点执行顺序
     * 从开始节点开始，按边的连接关系确定执行顺序
     */
    private List<Workflow.WorkflowNode> buildExecutionOrder(Workflow workflow) {
        List<Workflow.WorkflowNode> nodes = workflow.getNodes();
        List<Workflow.WorkflowEdge> edges = workflow.getEdges();

        // 找到开始节点
        Workflow.WorkflowNode startNode = nodes.stream()
            .filter(node -> "start".equals(node.getType()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("工作流必须有开始节点"));

        // 构建邻接表
        Map<String, String> nextNodeMap = new HashMap<>();
        for (Workflow.WorkflowEdge edge : edges) {
            nextNodeMap.put(edge.getSource(), edge.getTarget());
        }

        // 构建节点映射
        Map<String, Workflow.WorkflowNode> nodeMap = nodes.stream()
            .collect(Collectors.toMap(Workflow.WorkflowNode::getId, node -> node));

        // 按顺序遍历节点
        List<Workflow.WorkflowNode> executionOrder = new ArrayList<>();
        String currentNodeId = startNode.getId();

        while (currentNodeId != null) {
            Workflow.WorkflowNode currentNode = nodeMap.get(currentNodeId);
            if (currentNode == null) {
                break;
            }
            
            executionOrder.add(currentNode);
            
            // 如果是结束节点，停止遍历
            if ("end".equals(currentNode.getType())) {
                break;
            }
            
            // 获取下一个节点
            currentNodeId = nextNodeMap.get(currentNodeId);
        }

        return executionOrder;
    }

    /**
     * 验证工作流
     */
    private void validateWorkflow(Workflow workflow) {
        List<Workflow.WorkflowNode> nodes = workflow.getNodes();
        List<Workflow.WorkflowEdge> edges = workflow.getEdges();

        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("工作流节点不能为空");
        }

        if (edges == null || edges.isEmpty()) {
            throw new IllegalArgumentException("工作流边不能为空");
        }

        // 验证开始节点
        long startNodeCount = nodes.stream()
            .filter(node -> "start".equals(node.getType()))
            .count();
        if (startNodeCount != 1) {
            throw new IllegalArgumentException("工作流必须有且仅有一个开始节点");
        }

        // 验证结束节点
        long endNodeCount = nodes.stream()
            .filter(node -> "end".equals(node.getType()))
            .count();
        if (endNodeCount != 1) {
            throw new IllegalArgumentException("工作流必须有且仅有一个结束节点");
        }

        // 验证边的有效性
        Set<String> nodeIds = nodes.stream()
            .map(Workflow.WorkflowNode::getId)
            .collect(Collectors.toSet());
        
        for (Workflow.WorkflowEdge edge : edges) {
            if (!nodeIds.contains(edge.getSource())) {
                throw new IllegalArgumentException("边的源节点不存在: " + edge.getSource());
            }
            if (!nodeIds.contains(edge.getTarget())) {
                throw new IllegalArgumentException("边的目标节点不存在: " + edge.getTarget());
            }
        }
    }

    /**
     * 判断工作流是否配置为失败停止
     */
    private boolean isStopOnError(Workflow workflow) {
        // 默认失败时停止
        return true;
    }

    /**
     * 执行回调接口
     */
    public interface ExecutionCallback {
        void onComplete(WorkflowExecutionResult result);
        void onError(Exception e);
    }
}
