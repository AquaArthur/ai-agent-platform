package org.demo.core.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.core.model.entity.Workflow;

import java.util.Map;

/**
 * 节点配置工厂类
 * 根据节点类型将 WorkflowNode.config 映射到对应的配置类
 */
public class NodeConfigFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据节点类型创建对应的配置对象
     *
     * @param node 工作流节点
     * @return 对应的节点配置对象
     * @throws IllegalArgumentException 不支持的节点类型
     */
    public static BaseNodeConfig createConfig(Workflow.WorkflowNode node) {
        if (node == null || node.getType() == null) {
            throw new IllegalArgumentException("节点或节点类型不能为空");
        }

        Map<String, Object> config = node.getConfig();
        String type = node.getType();

        try {
            BaseNodeConfig nodeConfig;
            switch (type) {
                case "start":
                    nodeConfig = objectMapper.convertValue(config, StartNodeConfig.class);
                    break;
                case "end":
                    nodeConfig = objectMapper.convertValue(config, EndNodeConfig.class);
                    break;
                case "llm":
                    nodeConfig = objectMapper.convertValue(config, LLMNodeConfig.class);
                    break;
                case "http":
                    nodeConfig = objectMapper.convertValue(config, HttpNodeConfig.class);
                    break;
                case "knowledge":
                    nodeConfig = objectMapper.convertValue(config, KnowledgeNodeConfig.class);
                    break;
                case "intent":
                    nodeConfig = objectMapper.convertValue(config, IntentNodeConfig.class);
                    break;
                case "string":
                    nodeConfig = objectMapper.convertValue(config, StringNodeConfig.class);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的节点类型: " + type);
            }

            // 验证配置
            nodeConfig.validate();
            return nodeConfig;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("节点配置格式错误: " + e.getMessage(), e);
        }
    }
}
