package org.demo.core.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库响应 VO
 */
@Data
public class KnowledgeBaseVO {

    /**
     * 知识库唯一标识
     */
    private String id;

    /**
     * UUID标识
     */
    private String uuid;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识库图标URL
     */
    private String icon;

    /**
     * 作用域类型
     */
    private String scopeType;

    /**
     * 作用域ID
     */
    private Integer scopeId;

    /**
     * 父知识库ID
     */
    private String parentKbId;

    /**
     * 创建者ID
     */
    private String ownerId;

    /**
     * 访问级别
     */
    private String accessLevel;

    /**
     * 文档数量
     */
    private Integer documentCount;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 总文件大小
     */
    private Long totalSize;

    /**
     * 分块大小
     */
    private Integer chunkSize;

    /**
     * 分块重叠
     */
    private Integer chunkOverlap;

    /**
     * 向量模型
     */
    private String embeddingModel;

    /**
     * 向量模型ID
     */
    private String embeddingModelId;

    /**
     * 检索配置
     */
    private Object retrievalConfig;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
