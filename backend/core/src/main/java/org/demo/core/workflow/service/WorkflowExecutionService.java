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

    /**
     * 同步执行工作流
     *
     * @param workflowId 工作流ID
     * @param input 输入参数
     * @param userId 执行用户ID
     * @param llmModelId LLM模型ID
     * @return 执行记录ID
     */
    public String executeWorkflow(String workflowId, Map<String, Object> input, String userId, String llmModelId) {
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

        log.info("开始执行工作流: workflowId={}, executionId={}", workflowId, executionId);

        try {
            // 3. 更新状态为运行中
            execution.setStatus("running");
            workflowExecutionMapper.updateById(execution);

            // 4. 执行工作流
            WorkflowExecutionResult result = workflowExecutor.execute(workflow, input, llmModelId);

            // 5. 更新执行结果
            updateExecutionResult(execution, result);

            log.info("工作流执行完成: executionId={}, status={}", executionId, result.getStatus());

        } catch (Exception e) {
            log.error("工作流执行失败: executionId={}", executionId, e);
            
            // 更新执行状态为失败
            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            execution.setExecutionTime(calculateExecutionTime(execution.getStartedAt(), execution.getCompletedAt()));
            workflowExecutionMapper.updateById(execution);
        }

        return executionId;
    }

    /**
     * 异步执行工作流
     *
     * @param workflowId 工作流ID
     * @param input 输入参数
     * @param userId 执行用户ID
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
        workflowExecutor.executeAsync(workflow, input, llmModelId, new WorkflowExecutor.ExecutionCallback() {
            @Override
            public void onComplete(WorkflowExecutionResult result) {
                try {
                    // 更新执行状态为运行中
                    execution.setStatus("running");
                    workflowExecutionMapper.updateById(execution);

                    // 更新执行结果
                    updateExecutionResult(execution, result);
                    
                    log.info("异步工作流执行完成: executionId={}, status={}", executionId, result.getStatus());
                } catch (Exception e) {
                    log.error("更新异步工作流执行结果失败: executionId={}", executionId, e);
                }
            }

            @Override
            public void onError(Exception e) {
                log.error("异步工作流执行失败: executionId={}", executionId, e);
                
                // 更新执行状态为失败
                execution.setStatus("failed");
                execution.setErrorMessage(e.getMessage());
                execution.setCompletedAt(LocalDateTime.now());
                execution.setExecutionTime(calculateExecutionTime(execution.getStartedAt(), execution.getCompletedAt()));
                workflowExecutionMapper.updateById(execution);
            }
        });

        return executionId;
    }

    /**
     * 查询执行记录
     *
     * @param executionId 执行ID
     * @return 执行记录
     */
    public WorkflowExecution getExecution(String executionId) {
        return workflowExecutionMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WorkflowExecution>()
                .eq("execution_id", executionId)
        );
    }

    /**
     * 查询工作流的执行历史
     *
     * @param workflowId 工作流ID
     * @param limit 返回记录数
     * @return 执行记录列表
     */
    public List<WorkflowExecution> getExecutionHistory(String workflowId, Integer limit) {
        return workflowExecutionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WorkflowExecution>()
                .eq("workflow_id", workflowId)
                .orderByDesc("started_at")
                .last("LIMIT " + (limit != null ? limit : 10))
        );
    }

    /**
     * 终止工作流执行
     * 注意：当前实现不支持真正的终止，只是更新状态
     *
     * @param executionId 执行ID
     */
    public void terminateExecution(String executionId) {
        WorkflowExecution execution = getExecution(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("执行记录不存在: " + executionId);
        }

        if ("completed".equals(execution.getStatus()) || "failed".equals(execution.getStatus())) {
            throw new IllegalStateException("工作流已完成，无法终止");
        }

        execution.setStatus("terminated");
        execution.setCompletedAt(LocalDateTime.now());
        execution.setExecutionTime(calculateExecutionTime(execution.getStartedAt(), execution.getCompletedAt()));
        workflowExecutionMapper.updateById(execution);

        log.info("工作流执行已终止: executionId={}", executionId);
    }

    /**
     * 更新执行结果
     */
    private void updateExecutionResult(WorkflowExecution execution, WorkflowExecutionResult result) {
        execution.setStatus(result.getStatus());
        execution.setOutput(result.getOutput());
        execution.setErrorMessage(result.getErrorMessage());
        execution.setCompletedAt(LocalDateTime.now());
        execution.setExecutionTime(calculateExecutionTime(execution.getStartedAt(), execution.getCompletedAt()));

        // 转换节点执行记录
        List<WorkflowExecution.NodeExecution> nodeExecutions = new ArrayList<>();
        if (result.getNodeExecutions() != null) {
            for (NodeExecutionRecord record : result.getNodeExecutions()) {
                WorkflowExecution.NodeExecution nodeExecution = new WorkflowExecution.NodeExecution();
                nodeExecution.setNodeId(record.getNodeId());
                nodeExecution.setStatus(record.getStatus());
                nodeExecution.setOutput(record.getOutput());
                nodeExecution.setErrorMessage(record.getErrorMessage());
                nodeExecution.setStartedAt(record.getStartTime());
                nodeExecution.setCompletedAt(record.getEndTime());
                nodeExecution.setExecutionTime((int) record.getExecutionTimeMs());
                nodeExecutions.add(nodeExecution);
            }
        }
        execution.setNodeExecutions(nodeExecutions);

        workflowExecutionMapper.updateById(execution);
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
