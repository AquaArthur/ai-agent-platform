package org.demo.core.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.demo.core.mapper.WorkflowExecutionMapper;
import org.demo.core.mapper.WorkflowMapper;
import org.demo.core.model.entity.Workflow;
import org.demo.core.model.entity.WorkflowExecution;
import org.demo.core.workflow.executor.NodeExecutionRecord;
import org.demo.core.workflow.executor.WorkflowExecutionResult;
import org.demo.core.workflow.executor.WorkflowExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工作流执行服务
 * 负责工作流的启动、状态管理和结果记录
 */
@Slf4j
@Service
public class WorkflowExecutionService {

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    private WorkflowExecutionMapper workflowExecutionMapper;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    // 用于异步执行的线程池
    private final java.util.concurrent.ExecutorService executorService =
            java.util.concurrent.Executors.newCachedThreadPool();


    /**
     * 异步执行工作流
     *
     * @param workflowId 工作流ID
     * @param input      输入参数
     * @param userId     执行用户ID
     * @param llmModelId LLM模型ID
     * @return 执行记录ID
     */
    public String executeWorkflowAsync(String workflowId, Map<String, Object> input, String userId, String llmModelId) {
        // 1. 查询工作流定义
        Workflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在: " + workflowId);
        }

        // 2. 创建执行记录
        String executionId = UUID.randomUUID().toString();
        WorkflowExecution execution = new WorkflowExecution();
        execution.setExecutionId(executionId);
        execution.setWorkflowId(workflowId);
        execution.setUserId(userId);
        execution.setStatus("pending");
        execution.setInput(input);
        execution.setStartedAt(LocalDateTime.now());
        execution.setRunType("full");
        workflowExecutionMapper.insert(execution);

        log.info("提交异步执行工作流: workflowId={}, executionId={}", workflowId, executionId);

        // 3. 异步执行工作流
        // 注意：异步执行时，我们需要直接调用带回调的 execute 方法
        executorService.submit(() -> {
            WorkflowExecution currentExecution = null;
            try {
                log.info("开始异步执行工作流任务: executionId={}", executionId);
                
                // 从数据库重新获取execution对象，确保有正确的id
                currentExecution = workflowExecutionMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WorkflowExecution>()
                                .eq("execution_id", executionId)
                );

                if (currentExecution == null) {
                    log.error("无法找到执行记录: executionId={}", executionId);
                    return;
                }

                // 更新执行状态为运行中
                log.info("更新执行状态为running: executionId={}", executionId);
                currentExecution.setStatus("running");
                workflowExecutionMapper.updateById(currentExecution);

                // 执行工作流（带节点执行回调）
                log.info("调用工作流执行引擎: executionId={}", executionId);
                WorkflowExecutionResult result = workflowExecutor.execute(workflow, input, llmModelId,
                        new WorkflowExecutor.NodeExecutionCallback() {
                            @Override
                            public void onNodeCompleted(NodeExecutionRecord nodeRecord, List<NodeExecutionRecord> allNodeRecords) {
                                log.info("节点执行回调触发: nodeId={}, executionId={}", nodeRecord.getNodeId(), executionId);
                                // 暂时禁用实时更新，避免并发更新问题
                                // TODO: 后续可以使用消息队列或者批量更新来优化
                                log.info("跳过实时更新，等待最终更新: nodeId={}", nodeRecord.getNodeId());
                            }
                        });

                log.info("工作流执行引擎完成: executionId={}, status={}", executionId, result.getStatus());
                
                // 重新查询最新的execution对象，确保最终更新使用最新数据
                WorkflowExecution finalExecution = workflowExecutionMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WorkflowExecution>()
                                .eq("execution_id", executionId)
                );

                if (finalExecution != null) {
                    log.info("准备更新最终执行结果: executionId={}, status={}", executionId, result.getStatus());
                    // 更新执行结果
                    updateExecutionResult(finalExecution, result);
                    log.info("异步工作流执行完成并已更新: executionId={}, status={}", executionId, result.getStatus());
                } else {
                    log.error("最终更新时无法找到执行记录: executionId={}", executionId);
                }
            } catch (Exception e) {
                log.error("异步工作流执行失败: executionId={}, 异常信息: {}", executionId, e.getMessage(), e);

                // 重新查询execution对象进行失败更新
                if (currentExecution == null) {
                    currentExecution = workflowExecutionMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WorkflowExecution>()
                                    .eq("execution_id", executionId)
                    );
                }

                if (currentExecution != null) {
                    // 更新执行状态为失败
                    currentExecution.setStatus("failed");
                    currentExecution.setErrorMessage(e.getMessage());
                    currentExecution.setCompletedAt(LocalDateTime.now());
                    currentExecution.setExecutionTime(calculateExecutionTime(currentExecution.getStartedAt(), currentExecution.getCompletedAt()));
                    workflowExecutionMapper.updateById(currentExecution);
                }
            }
        });

        return executionId;
    }

    /**
     * 更新执行结果
     */
    private void updateExecutionResult(WorkflowExecution execution, WorkflowExecutionResult result) {
        log.info("updateExecutionResult 开始: executionId={}, status={}", execution.getExecutionId(), result.getStatus());
        
        execution.setStatus(result.getStatus());
        execution.setOutput(result.getOutput());
        execution.setErrorMessage(result.getErrorMessage());
        execution.setCompletedAt(LocalDateTime.now());
        execution.setExecutionTime(calculateExecutionTime(execution.getStartedAt(), execution.getCompletedAt()));

        log.info("updateExecutionResult 设置字段完成: executionId={}, status={}, output={}", 
            execution.getExecutionId(), execution.getStatus(), execution.getOutput());

        // 转换节点执行记录
        List<WorkflowExecution.NodeExecution> nodeExecutions = new ArrayList<>();
        if (result.getNodeExecutions() != null) {
            log.info("转换节点执行记录: 共{}个节点", result.getNodeExecutions().size());
            for (NodeExecutionRecord record : result.getNodeExecutions()) {
                WorkflowExecution.NodeExecution nodeExecution = new WorkflowExecution.NodeExecution();
                nodeExecution.setNodeId(record.getNodeId());
                nodeExecution.setStatus(record.getStatus());

                // 转换输入为 Map 类型
                if (record.getInput() != null) {
                    if (record.getInput() instanceof Map) {
                        nodeExecution.setInput((Map<String, Object>) record.getInput());
                    } else {
                        // 如果不是 Map，包装成 Map
                        Map<String, Object> inputMap = new java.util.HashMap<>();
                        inputMap.put("config", record.getInput());
                        nodeExecution.setInput(inputMap);
                    }
                }

                // 转换输出为 Map 类型
                if (record.getOutput() != null) {
                    if (record.getOutput() instanceof Map) {
                        nodeExecution.setOutput((Map<String, Object>) record.getOutput());
                    } else {
                        // 如果不是 Map，包装成 Map
                        Map<String, Object> outputMap = new java.util.HashMap<>();
                        outputMap.put("result", record.getOutput());
                        nodeExecution.setOutput(outputMap);
                    }
                }

                // 设置错误信息（字段名是 error，不是 errorMessage）
                nodeExecution.setError(record.getErrorMessage());

                // 转换时间为字符串格式
                if (record.getStartTime() != null) {
                    nodeExecution.setStartedAt(record.getStartTime().toString());
                }
                if (record.getEndTime() != null) {
                    nodeExecution.setCompletedAt(record.getEndTime().toString());
                }

                nodeExecutions.add(nodeExecution);
            }
        }
        execution.setNodeExecutions(nodeExecutions);

        log.info("准备调用 updateById: executionId={}, status={}", execution.getExecutionId(), execution.getStatus());
        int updateCount = workflowExecutionMapper.updateById(execution);
        log.info("updateById 完成: executionId={}, 影响行数={}", execution.getExecutionId(), updateCount);
    }

    /**
     * 实时更新节点执行状态
     * 注意：只更新 node_executions 字段，不影响其他字段
     */
    private void updateNodeExecutions(WorkflowExecution execution, List<NodeExecutionRecord> nodeRecords) {
        try {
            // 转换节点执行记录
            List<WorkflowExecution.NodeExecution> nodeExecutions = new ArrayList<>();
            for (NodeExecutionRecord record : nodeRecords) {
            WorkflowExecution.NodeExecution nodeExecution = new WorkflowExecution.NodeExecution();
            nodeExecution.setNodeId(record.getNodeId());
            nodeExecution.setStatus(record.getStatus());

            // 转换输出为 Map 类型
            if (record.getOutput() != null) {
                if (record.getOutput() instanceof Map) {
                    nodeExecution.setOutput((Map<String, Object>) record.getOutput());
                } else {
                    // 如果不是 Map，包装成 Map
                    Map<String, Object> outputMap = new java.util.HashMap<>();
                    outputMap.put("result", record.getOutput());
                    nodeExecution.setOutput(outputMap);
                }
            }

            // 设置错误信息（字段名是 error，不是 errorMessage）
            nodeExecution.setError(record.getErrorMessage());

            // 转换时间为字符串格式
            if (record.getStartTime() != null) {
                nodeExecution.setStartedAt(record.getStartTime().toString());
            }
            if (record.getEndTime() != null) {
                nodeExecution.setCompletedAt(record.getEndTime().toString());
            }

            nodeExecutions.add(nodeExecution);
        }

        // 使用 UpdateWrapper 只更新 node_executions 字段，避免覆盖其他字段
        log.info("准备更新节点执行状态: executionId={}, nodeCount={}", execution.getExecutionId(), nodeExecutions.size());
        
        WorkflowExecution updateEntity = new WorkflowExecution();
        updateEntity.setNodeExecutions(nodeExecutions);
        
            log.info("调用 mapper.update: executionId={}", execution.getExecutionId());
            int updateCount = workflowExecutionMapper.update(updateEntity, 
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<WorkflowExecution>()
                    .eq("execution_id", execution.getExecutionId())
            );
            
            log.info("已更新节点执行状态: executionId={}, nodeCount={}, 影响行数={}", 
                execution.getExecutionId(), nodeExecutions.size(), updateCount);
        } catch (Exception e) {
            log.error("更新节点执行状态失败: executionId={}, 异常: {}", execution.getExecutionId(), e.getMessage(), e);
            throw e; // 重新抛出异常
        }
    }

    /**
     * 计算执行时长（毫秒）
     */
    private Integer calculateExecutionTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (int) Duration.between(startTime, endTime).toMillis();
    }
}
