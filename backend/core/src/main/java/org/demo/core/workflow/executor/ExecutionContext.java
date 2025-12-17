package org.demo.core.workflow.executor;

import java.util.Map;

/**
 * 节点执行上下文
 * 存储工作流执行过程中的所有数据
 */
public class ExecutionContext {
    /**
     * 工作流输入参数
     */
    private Map<String, Object> input;

    /**
     * 节点输出结果
     * key: 节点ID
     * value: 节点输出
     */
    private Map<String, Object> nodeOutputs;

    /**
     * LLM模型ID（用于LLM和Intent节点）
     */
    private String llmModelId;

    /**
     * Agent ID（用于获取智能体信息）
     */
    private String agentId;

    public ExecutionContext(Map<String, Object> input, Map<String, Object> nodeOutputs) {
        this.input = input;
        this.nodeOutputs = nodeOutputs;
    }

    public ExecutionContext(Map<String, Object> input, Map<String, Object> nodeOutputs, String llmModelId) {
        this.input = input;
        this.nodeOutputs = nodeOutputs;
        this.llmModelId = llmModelId;
    }

    public ExecutionContext(Map<String, Object> input, Map<String, Object> nodeOutputs, String llmModelId, String agentId) {
        this.input = input;
        this.nodeOutputs = nodeOutputs;
        this.llmModelId = llmModelId;
        this.agentId = agentId;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public Map<String, Object> getNodeOutputs() {
        return nodeOutputs;
    }

    public String getLlmModelId() {
        return llmModelId;
    }

    public void setLlmModelId(String llmModelId) {
        this.llmModelId = llmModelId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * 保存节点输出
     */
    public void saveNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }

    /**
     * 获取节点输出
     */
    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }
}
