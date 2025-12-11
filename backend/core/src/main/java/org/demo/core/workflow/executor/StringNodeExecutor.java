package org.demo.core.workflow.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.Workflow;
import org.demo.core.workflow.node.BaseNodeConfig;
import org.demo.core.workflow.node.StringNodeConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字符串处理节点执行器
 * 处理字符串数据（拼接、替换、格式化等）
 */
@Slf4j
@Component
public class StringNodeExecutor implements NodeExecutor {

    @Override
    public Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) throws Exception {
        log.info("执行String节点: {}", node.getId());
        
        StringNodeConfig stringConfig = (StringNodeConfig) config;
        String inputString = stringConfig.getInputString();
        String operation = stringConfig.getOperation();
        Map<String, Object> parameters = stringConfig.getParameters();
        
        String result;
        
        switch (operation) {
            case "concat":
                result = handleConcat(inputString, parameters);
                break;
            case "replace":
                result = handleReplace(inputString, parameters);
                break;
            case "substring":
                result = handleSubstring(inputString, parameters);
                break;
            case "format":
                result = handleFormat(inputString, parameters);
                break;
            case "trim":
                result = inputString.trim();
                break;
            case "upper":
                result = inputString.toUpperCase();
                break;
            case "lower":
                result = inputString.toLowerCase();
                break;
            default:
                throw new IllegalArgumentException("不支持的字符串操作: " + operation);
        }
        
        Map<String, Object> output = new HashMap<>();
        output.put("output", result);
        output.put("operation", operation);
        
        return output;
    }

    /**
     * 拼接字符串
     */
    private String handleConcat(String inputString, Map<String, Object> parameters) {
        if (parameters == null) {
            return inputString;
        }
        
        String separator = (String) parameters.getOrDefault("separator", "");
        @SuppressWarnings("unchecked")
        List<String> strings = (List<String>) parameters.get("strings");
        
        if (strings == null || strings.isEmpty()) {
            return inputString;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(strings.get(i));
        }
        
        return sb.toString();
    }

    /**
     * 替换字符串
     */
    private String handleReplace(String inputString, Map<String, Object> parameters) {
        if (parameters == null) {
            return inputString;
        }
        
        String target = (String) parameters.get("target");
        String replacement = (String) parameters.get("replacement");
        
        if (target == null || replacement == null) {
            return inputString;
        }
        
        return inputString.replace(target, replacement);
    }

    /**
     * 截取字符串
     */
    private String handleSubstring(String inputString, Map<String, Object> parameters) {
        if (parameters == null) {
            return inputString;
        }
        
        Integer start = (Integer) parameters.get("start");
        Integer end = (Integer) parameters.get("end");
        
        if (start == null) {
            start = 0;
        }
        if (end == null) {
            end = inputString.length();
        }
        
        // 确保索引在合法范围内
        start = Math.max(0, Math.min(start, inputString.length()));
        end = Math.max(start, Math.min(end, inputString.length()));
        
        return inputString.substring(start, end);
    }

    /**
     * 格式化字符串
     */
    private String handleFormat(String inputString, Map<String, Object> parameters) {
        if (parameters == null) {
            return inputString;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, String> values = (Map<String, String>) parameters.get("values");
        
        if (values == null) {
            return inputString;
        }
        
        String result = inputString;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return result;
    }

    @Override
    public String getSupportedType() {
        return "string";
    }
}
