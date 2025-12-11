package org.demo.core.workflow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库检索节点配置
 * 从知识库中检索相关内容
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeNodeConfig extends BaseNodeConfig {
    /**
     * 知识库ID（必填）
     */
    private Long knowledgeBaseId;

    /**
     * 查询文本，支持变量替换（必填）
     */
    private String query;

    /**
     * 返回最相似的K个文档块，范围1-10，默认5
     */
    private Integer topK = 5;

    /**
     * 相似度阈值，范围0-1，默认0.7
     */
    private Double similarityThreshold = 0.7;

    public KnowledgeNodeConfig() {
        setType("knowledge");
    }

    @Override
    public void validate() {
        if (knowledgeBaseId == null) {
            throw new IllegalArgumentException("Knowledge节点的 knowledgeBaseId 不能为空");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Knowledge节点的 query 不能为空");
        }
        if (topK != null && (topK < 1 || topK > 10)) {
            throw new IllegalArgumentException("Knowledge节点的 topK 必须在 1-10 之间");
        }
        if (similarityThreshold != null && (similarityThreshold < 0 || similarityThreshold > 1)) {
            throw new IllegalArgumentException("Knowledge节点的 similarityThreshold 必须在 0-1 之间");
        }
    }
}
