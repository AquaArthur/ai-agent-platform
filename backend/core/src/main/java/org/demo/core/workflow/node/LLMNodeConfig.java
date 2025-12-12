package org.demo.core.workflow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * LLM节点配置
 * 调用大模型生成文本
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LLMNodeConfig extends BaseNodeConfig {
    /**
     * 关联的智能体UUID（必填）
     */
    private String agentUuid;

    /**
     * 提示词，支持变量替换（必填）
     */
    private String prompt;

    /**
     * 温度参数，范围0-2，默认0.7
     */
    private Double temperature = 0.7;

    /**
     * 最大生成token数，默认2000
     */
    private Integer maxTokens = 2000;

    public LLMNodeConfig() {
        setType("llm");
    }

    @Override
    public void validate() {
        if (agentUuid == null || agentUuid.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM节点的 agentUuid 不能为空");
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM节点的 prompt 不能为空");
        }
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("LLM节点的 temperature 必须在 0-2 之间");
        }
        if (maxTokens != null && maxTokens <= 0) {
            throw new IllegalArgumentException("LLM节点的 maxTokens 必须大于 0");
        }
    }
}
