package org.demo.core.workflow.executor;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行结果
 * 包含工作流执行的完整信息
 */
@Data
public class WorkflowExecutionResult {
    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 输出结果
     */
    private Map<String, Object> output;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 节点执行记录列表
     */
    private List<NodeExecutionRecord> nodeExecutions;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 获取执行时长（毫秒）
     */
    public long getExecutionTimeMs() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
}
