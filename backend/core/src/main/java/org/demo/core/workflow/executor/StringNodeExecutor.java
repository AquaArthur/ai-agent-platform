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
     * 支持负数索引：-1 表示最后一个字符，-5 表示倒数第5个字符
     */
    private String handleSubstring(String inputString, Map<String, Object> parameters) {
        if (parameters == null) {
            return inputString;
        }
        
        Integer start = (Integer) parameters.get("start");
        Integer end = (Integer) parameters.get("end");
        
        int length = inputString.length();
        
        // 处理 start 参数（支持负数索引）
        if (start == null) {
            start = 0;
        } else if (start < 0) {
            // 负数索引：从末尾开始计数
            start = Math.max(0, length + start);
        } else {
            // 正数索引：确保不超过字符串长度
            start = Math.min(start, length);
        }
        
        // 处理 end 参数（支持负数索引）
        if (end == null) {
            end = length;
        } else if (end < 0) {
            // 负数索引：从末尾开始计数
            end = Math.max(0, length + end);
        } else {
            // 正数索引：确保不超过字符串长度
            end = Math.min(end, length);
        }
        
        // 确保 start <= end
        if (start > end) {
            return "";
        }
        
        return inputString.substring(start, end);
    }

    /**
     * 格式化字符串
     * 将 inputString 中的占位符（双大括号格式 {{key}}）替换为 values 中的值
     * 使用双大括号避免与工作流变量替换（单大括号 {node_id}）冲突
     */
    private String handleFormat(String inputString, Map<String, Object> parameters) {
        if (parameters == null) {
            log.warn("format操作的parameters为null");
            return inputString;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> values = (Map<String, Object>) parameters.get("values");
        
        if (values == null) {
            log.warn("format操作的values为null，inputString={}", inputString);
            return inputString;
        }
        
        log.info("format操作 - inputString={}, values={}", inputString, values);
        
        String result = inputString;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            String strValue;
            
            // 如果值是 Map 且包含 output 字段，提取 output 的值（与 VariableResolver 一致）
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                if (mapValue.containsKey("output")) {
                    strValue = String.valueOf(mapValue.get("output"));
                } else {
                    strValue = String.valueOf(value);
                }
            } else {
                strValue = value != null ? String.valueOf(value) : "";
            }
            
            log.info("替换占位符 {{{{{}}}}}} 为 [{}]", entry.getKey(), strValue);
            // 使用双大括号语法 {{key}}
            result = result.replace("{{" + entry.getKey() + "}}", strValue);
        }
        
        log.info("format操作结果: {}", result);
        return result;
    }

    @Override
    public String getSupportedType() {
        return "string";
    }
}
