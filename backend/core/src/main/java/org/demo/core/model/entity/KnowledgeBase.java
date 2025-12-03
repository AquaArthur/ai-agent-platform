package org.demo.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库表实体类
 * 功能: 存储知识库的基本信息，支持分级设计
 * 关联用户故事: US-006, US-008, US-010
 */
@Data
@TableName(value = "knowledge_base", autoResultMap = true)
public class KnowledgeBase {

    /**
     * 知识库唯一标识
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * UUID标识
     */
    @TableField("uuid")
    private String uuid;

    /**
     * 知识库名称（必填）
     */
    @TableField("name")
    private String name;

    /**
     * 知识库描述
     */
    @TableField("description")
    private String description;

    /**
     * 知识库图标URL
     */
    @TableField("icon")
    private String icon;

    /**
     * 作用域类型（system/school/course/agent/personal）
     */
    @TableField("scope_type")
    private String scopeType;

    /**
     * 作用域ID
     */
    @TableField("scope_id")
    private Integer scopeId;

    /**
     * 父知识库ID
     */
    @TableField("parent_kb_id")
    private String parentKbId;

    /**
     * 创建者ID
     */
    @TableField("owner_id")
    private String ownerId;

    /**
     * 创建者ID（别名字段）
     */
    @TableField("user_id")
    private String userId;

    /**
     * 访问级别（public/protected/private）
     */
    @TableField("access_level")
    private String accessLevel;

    /**
     * 文档数量
     */
    @TableField("document_count")
    private Integer documentCount;

    /**
     * 分块数量
     */
    @TableField("chunk_count")
    private Integer chunkCount;

    /**
     * 总文件大小（字节）
     */
    @TableField("total_size")
    private Long totalSize;

    /**
     * 分块大小
     */
    @TableField("chunk_size")
    private Integer chunkSize;

    /**
     * 分块重叠
     */
    @TableField("chunk_overlap")
    private Integer chunkOverlap;

    /**
     * 向量模型（如text-embedding-3）
     */
    @TableField("embedding_model")
    private String embeddingModel;

    /**
     * 向量模型ID（外键）
     */
    @TableField("embedding_model_id")
    private String embeddingModelId;

    /**
     * 检索配置
     */
    @TableField(value = "retrieval_config", typeHandler = JacksonTypeHandler.class)
    private Object retrievalConfig;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
