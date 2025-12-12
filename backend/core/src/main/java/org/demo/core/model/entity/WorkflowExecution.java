package org.demo.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行历史实体类
 * 对应数据库表: workflow_execution
 */
@Data
@TableName(value = "workflow_execution", autoResultMap = true)
public class WorkflowExecution {

    /**
     * 执行记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 执行UUID（用于外部查询）
     */
    @TableField("execution_id")
    private String executionId;

    /**
     * 工作流ID
     */
    @TableField("workflow_id")
    private String workflowId;

    /**
     * 执行者ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 状态（pending/running/completed/failed/terminated）
     */
    @TableField("status")
    private String status;

    /**
     * 初始输入参数
     */
    @TableField(value = "input", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> input;

    /**
     * 最终输出结果
     */
    @TableField(value = "output", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> output;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 节点执行快照（各节点的执行记录）
     */
    @TableField(value = "node_executions", typeHandler = JacksonTypeHandler.class)
    private List<NodeExecution> nodeExecutions;

    /**
     * 类型（full-完整执行/debug-调试执行）
     */
    @TableField("run_type")
    private String runType;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    /**
     * 执行耗时（毫秒）
     */
    @TableField("execution_time")
    private Integer executionTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 节点执行记录静态内部类
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeExecution {
        /**
         * 节点ID
         */
        @JsonAlias({"node_id", "nodeId"})
        @JsonProperty("node_id")
        private String nodeId;

        /**
         * 节点执行状态
         */
        @JsonAlias({"status"})
        @JsonProperty("status")
        private String status;

        /**
         * 节点开始时间
         */
        @JsonAlias({"started_at", "startedAt", "startTime"})
        @JsonProperty("started_at")
        private String startedAt;

        /**
         * 节点完成时间
         */
        @JsonAlias({"completed_at", "completedAt", "endTime"})
        @JsonProperty("completed_at")
        private String completedAt;

        /**
         * 节点输入
         */
        @JsonAlias({"input"})
        @JsonProperty("input")
        private Map<String, Object> input;

        /**
         * 节点输出
         */
        @JsonAlias({"output"})
        @JsonProperty("output")
        private Map<String, Object> output;

        /**
         * 节点错误信息（如有）
         */
        @JsonAlias({"error"})
        @JsonProperty("error")
        private String error;
    }
}
