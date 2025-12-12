package org.demo.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流实体类
 * 对应数据库表: workflow
 */
@Data
@TableName(value = "workflow", autoResultMap = true)
public class Workflow {

    /**
     * 工作流唯一标识
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 工作流UUID（用于外部接口）
     */
    @TableField("uuid")
    private String uuid;

    /**
     * 所属智能体ID
     */
    @TableField("agent_id")
    private String agentId;

    /**
     * 工作流名称
     */
    @TableField("name")
    private String name;

    /**
     * 工作流描述
     */
    @TableField("description")
    private String description;

    /**
     * 节点列表（存储节点配置信息）
     */
    @TableField(value = "nodes", typeHandler = JacksonTypeHandler.class)
    private List<WorkflowNode> nodes;

    /**
     * 边列表（存储节点间连接关系）
     */
    @TableField(value = "edges", typeHandler = JacksonTypeHandler.class)
    private List<WorkflowEdge> edges;

    /**
     * 工作流配置（stop_on_error、timeout、retry_on_failure等）
     */
    @TableField(value = "config", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    /**
     * DAG校验是否通过
     */
    @TableField("is_valid")
    private Boolean isValid;

    /**
     * 工作流是否激活
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 工作流是否公开
     */
    @TableField("is_public")
    private Boolean isPublic;

    /**
     * 执行次数统计
     */
    @TableField("execution_count")
    private Integer executionCount;

    /**
     * 成功次数统计
     */
    @TableField("success_count")
    private Integer successCount;

    /**
     * 创建者ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 工作流节点静态内部类
     */
    @Data
    public static class WorkflowNode {
        /**
         * 节点唯一ID
         */
        private String id;

        /**
         * 节点类型（start、llm、http、knowledge、intent、string、end）
         */
        private String type;

        /**
         * 节点显示名称
         */
        private String label;

        /**
         * 节点在画布上的位置
         */
        private Position position;

        /**
         * 节点特定配置（根据type不同而不同）
         */
        private Map<String, Object> config;

        /**
         * 位置坐标
         */
        @Data
        public static class Position {
            /**
             * X坐标
             */
            private Integer x;

            /**
             * Y坐标
             */
            private Integer y;
        }
    }

    /**
     * 工作流边静态内部类
     */
    @Data
    public static class WorkflowEdge {
        /**
         * 边唯一ID
         */
        private String id;

        /**
         * 源节点ID
         */
        private String source;

        /**
         * 目标节点ID
         */
        private String target;
    }
}
