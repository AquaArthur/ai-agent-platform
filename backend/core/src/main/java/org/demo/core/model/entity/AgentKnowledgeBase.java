package org.demo.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体知识库关联实体类
 * 对应数据库表: agent_knowledge_base
 * 用于存储智能体与知识库的多对多关联关系
 */
@Data
@TableName(value = "agent_knowledge_base")
public class AgentKnowledgeBase {

    /**
     * 关联记录唯一标识
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 智能体ID
     */
    @TableField("agent_id")
    private String agentId;

    /**
     * 知识库ID
     */
    @TableField("knowledge_base_id")
    private String knowledgeBaseId;

    /**
     * 优先级（数值越大优先级越高，用于检索排序）
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 是否启用（支持临时禁用某个知识库而不删除关联）
     */
    @TableField("is_enabled")
    private Boolean isEnabled;

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
}
