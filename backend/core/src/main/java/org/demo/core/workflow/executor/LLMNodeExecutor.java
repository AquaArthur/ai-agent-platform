package org.demo.core.workflow.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.Agent;
import org.demo.core.model.entity.Workflow;
import org.demo.core.mapper.AgentMapper;
import org.demo.core.service.LlmService;
import org.demo.core.workflow.node.BaseNodeConfig;
import org.demo.core.workflow.node.LLMNodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM节点执行器
 * 调用大模型生成文本
 */
@Slf4j
@Component
public class LLMNodeExecutor implements NodeExecutor {

    @Autowired
    private LlmService llmService;

    @Autowired
    private AgentMapper agentMapper;

    @Override
    public Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) throws Exception {
        log.info("执行LLM节点: {}", node.getId());
        
        LLMNodeConfig llmConfig = (LLMNodeConfig) config;
        
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
        String llmModelId = llmConfig.getLlmModelId();
        if (llmModelId == null || llmModelId.isEmpty()) {
            throw new IllegalArgumentException("LLM节点未配置模型ID");
        }
        
        // 调用LLM服务
        String response = llmService.chat(
            agentId,
            llmModelId,
            llmConfig.getPrompt(),
            null // conversationHistory
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("output", response);
        result.put("agentId", agentId);
        result.put("llmModelId", llmModelId);
        result.put("prompt", llmConfig.getPrompt());
        
        return result;
    }

    @Override
    public String getSupportedType() {
        return "llm";
    }
}
