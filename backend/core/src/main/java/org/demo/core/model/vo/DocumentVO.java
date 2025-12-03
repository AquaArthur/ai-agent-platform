package org.demo.core.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档视图对象
 */
@Data
public class DocumentVO {

    /**
     * 文档UUID
     */
    private String uuid;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文档文件名
     */
    private String filename;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 处理状态（uploading/processing/processed/failed）
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 处理完成时间
     */
    private LocalDateTime processedAt;

    /**
     * 所属知识库ID
     */
    private String knowledgeBaseId;

    /**
     * 上传者ID
     */
    private String userId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
