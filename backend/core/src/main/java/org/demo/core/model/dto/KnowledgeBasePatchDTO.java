package org.demo.core.model.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * 知识库更新请求 DTO（部分更新）
 */
@Data
public class KnowledgeBasePatchDTO {

    /**
     * 知识库名称
     */
    @Size(max = 100, message = "知识库名称长度不能超过100个字符")
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
     * 访问级别（public/protected/private）
     */
    private String accessLevel;

    /**
     * 分块大小
     */
    @Min(value = 100, message = "chunk_size必须在100-2000之间")
    @Max(value = 2000, message = "chunk_size必须在100-2000之间")
    private Integer chunkSize;

    /**
     * 分块重叠
     */
    private Integer chunkOverlap;

    /**
     * 向量模型ID
     */
    private String embeddingModelId;

    /**
     * 检索配置
     */
    private Object retrievalConfig;
}
