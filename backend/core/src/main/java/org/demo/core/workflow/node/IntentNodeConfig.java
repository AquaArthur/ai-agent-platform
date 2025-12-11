package org.demo.core.workflow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 意图识别节点配置
 * 识别用户输入的意图
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IntentNodeConfig extends BaseNodeConfig {
    /**
     * 输入文本，支持变量替换（必填）
     */
    private String inputText;

    /**
     * 意图类别列表（必填）
     */
    private List<String> intentCategories;

    /**
     * 识别方式：llm（使用大模型识别）或 keyword（关键词匹配）
     */
    private String recognitionMethod = "llm";

    /**
     * 当使用llm方式时，关联的智能体UUID
     */
    private String agentUuid;

    /**
     * 当使用keyword方式时，每个意图对应的关键词列表
     * 格式：{"查询": ["查询", "搜索", "找"], "操作": ["操作", "执行", "运行"]}
     */
    private Map<String, List<String>> keywords;

    public IntentNodeConfig() {
        setType("intent");
    }

    @Override
    public void validate() {
        if (inputText == null || inputText.trim().isEmpty()) {
            throw new IllegalArgumentException("Intent节点的 inputText 不能为空");
        }
        if (intentCategories == null || intentCategories.isEmpty()) {
            throw new IllegalArgumentException("Intent节点的 intentCategories 不能为空");
        }
        if (recognitionMethod == null || 
            (!recognitionMethod.equals("llm") && !recognitionMethod.equals("keyword"))) {
            throw new IllegalArgumentException("Intent节点的 recognitionMethod 只支持 llm 或 keyword");
        }
        if ("llm".equals(recognitionMethod) && (agentUuid == null || agentUuid.trim().isEmpty())) {
            throw new IllegalArgumentException("使用 llm 识别方式时，agentUuid 不能为空");
        }
        if ("keyword".equals(recognitionMethod) && (keywords == null || keywords.isEmpty())) {
            throw new IllegalArgumentException("使用 keyword 识别方式时，keywords 不能为空");
        }
    }
}
