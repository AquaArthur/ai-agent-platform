package org.demo.core.workflow.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.core.mapper.AgentMapper;
import org.demo.core.model.entity.Agent;
import org.demo.core.model.entity.Workflow;
import org.demo.core.service.LlmService;
import org.demo.core.workflow.node.BaseNodeConfig;
import org.demo.core.workflow.node.IntentNodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图识别节点执行器
 * 识别用户输入的意图
 */
@Slf4j
@Component
public class IntentNodeExecutor implements NodeExecutor {

    @Autowired
    private LlmService llmService;

    @Autowired
    private AgentMapper agentMapper;

    @Override
    public Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) throws Exception {
        log.info("执行Intent节点: {}", node.getId());
        
        IntentNodeConfig intentConfig = (IntentNodeConfig) config;
        
        String recognizedIntent;
        double confidence;
        
        if ("llm".equals(intentConfig.getRecognitionMethod())) {
            // 使用LLM识别意图
            recognizedIntent = recognizeIntentByLLM(intentConfig, context);
            confidence = 0.9; // LLM识别默认高置信度
        } else {
            // 使用关键词匹配识别意图
            Map<String, Object> keywordResult = recognizeIntentByKeyword(intentConfig);
            recognizedIntent = (String) keywordResult.get("intent");
            confidence = (double) keywordResult.get("confidence");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("intent", recognizedIntent);
        result.put("confidence", confidence);
        result.put("inputText", intentConfig.getInputText());
        
        return result;
    }

    /**
     * 使用LLM识别意图
     */
    private String recognizeIntentByLLM(IntentNodeConfig config, ExecutionContext context) throws Exception {
        String prompt = String.format(
            "请识别以下文本的意图，从这些类别中选择一个：%s\n\n文本：%s\n\n请直接回答意图类别，不要有其他说明。",
            String.join("、", config.getIntentCategories()),
            config.getInputText()
        );
        
        // 从上下文获取智能体ID
        String agentId = context.getAgentId();
        if (agentId == null || agentId.isEmpty()) {
            throw new IllegalArgumentException("未指定智能体ID");
        }
        
        // 查询智能体验证存在性
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在: " + agentId);
        }
        
        // 从节点配置获取LLM模型ID
        String llmModelId = config.getLlmModelId();
        if (llmModelId == null || llmModelId.isEmpty()) {
            throw new IllegalArgumentException("Intent节点未配置模型ID");
        }
        
        // 调用LLM服务
        String response = llmService.chat(
            agentId,
            llmModelId,
            prompt,
            null
        );
        
        // 从响应中提取意图
        for (String category : config.getIntentCategories()) {
            if (response.contains(category)) {
                return category;
            }
        }
        
        return config.getIntentCategories().get(0); // 默认返回第一个类别
    }

    /**
     * 使用关键词匹配识别意图
     */
    private Map<String, Object> recognizeIntentByKeyword(IntentNodeConfig config) {
        String inputText = config.getInputText().toLowerCase();
        Map<String, List<String>> keywords = config.getKeywords();
        
        String matchedIntent = null;
        int maxMatches = 0;
        
        // 统计每个意图的关键词匹配数
        for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
            String intent = entry.getKey();
            List<String> intentKeywords = entry.getValue();
            
            int matches = 0;
            for (String keyword : intentKeywords) {
                if (inputText.contains(keyword.toLowerCase())) {
                    matches++;
                }
            }
            
            if (matches > maxMatches) {
                maxMatches = matches;
                matchedIntent = intent;
            }
        }
        
        // 如果没有匹配，返回第一个意图
        if (matchedIntent == null) {
            matchedIntent = config.getIntentCategories().get(0);
        }
        
        // 计算置信度（匹配关键词数 / 该意图总关键词数）
        double confidence = 0.0;
        if (matchedIntent != null && keywords.containsKey(matchedIntent)) {
            int totalKeywords = keywords.get(matchedIntent).size();
            confidence = totalKeywords > 0 ? (double) maxMatches / totalKeywords : 0.0;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("intent", matchedIntent);
        result.put("confidence", confidence);
        
        return result;
    }

    @Override
    public String getSupportedType() {
        return "intent";
    }
}
