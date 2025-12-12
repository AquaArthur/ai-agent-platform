package org.demo.core.workflow.executor;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点执行记录
 * 记录单个节点的执行信息
 */
@Data
public class NodeExecutionRecord {
    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 节点输入（解析变量后的配置）
     */
    private Object input;

    /**
     * 节点输出
     */
    private Object output;

    /**
     * 错误信息
     */
    private String errorMessage;

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
