package org.demo.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档表实体类
 * 功能: 存储知识库中上传的文档信息及处理状态
 * 关联用户故事: US-007, US-008
 */
@Data
@TableName(value = "document", autoResultMap = true)
public class Document {

    /**
     * 文档唯一标识
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 文档UUID（用于外部接口）
     */
    @TableField("uuid")
    private String uuid;

    /**
     * 文档名称（显示名称）
     */
    @TableField("name")
    private String name;

    /**
     * 文档文件名（原始文件名）
     */
    @TableField("filename")
    private String filename;

    /**
     * 文档文件名（别名字段）
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件存储URL（对象存储地址）
     */
    @TableField("file_url")
    private String fileUrl;

    /**
     * 文档存储路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 文档大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文档类型（txt/md/markdown）
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 切分片段数量
     */
    @TableField("chunk_count")
    private Integer chunkCount;

    /**
     * 处理状态（uploading/processing/processed/failed）
     */
    @TableField("status")
    private String status;

    /**
     * 处理状态数值（0-上传中, 1-处理中, 2-已完成, 3-失败）
     */
    @TableField("process_status")
    private Integer processStatus;

    /**
     * 处理失败原因
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 处理完成时间
     */
    @TableField("processed_at")
    private LocalDateTime processedAt;

    /**
     * 所属知识库ID
     */
    @TableField("knowledge_base_id")
    private String knowledgeBaseId;

    /**
     * 所属知识库ID（别名字段）
     */
    @TableField("kb_id")
    private String kbId;

    /**
     * 上传者ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 创建时间（别名字段）
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 更新时间（别名字段）
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
