package org.demo.core.workflow.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.workflow.executor.ExecutionContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量替换工具类
 * 支持 {node_id}、{node_id.field}、{input.param} 格式的变量替换
 */
@Slf4j
public class VariableResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 替换配置中的所有变量
     *
     * @param config 节点配置（可以是Map、List、String、JavaBean等）
     * @param context 执行上下文
     * @return 替换后的配置
     */
    public static Object resolveVariables(Object config, ExecutionContext context) {
        if (config == null) {
            return null;
        }

        if (config instanceof String) {
            return resolveString((String) config, context);
        } else if (config instanceof Map) {
            return resolveMap((Map<?, ?>) config, context);
        } else if (config instanceof List) {
            return resolveList((List<?>) config, context);
        } else if (isPrimitiveOrWrapper(config)) {
            // 基本类型和包装类型直接返回
            return config;
        } else {
            // 处理 JavaBean 对象（如 BaseNodeConfig）
            return resolveJavaBean(config, context);
        }
    }

    /**
     * 判断对象是否为基本类型或包装类型
     */
    private static boolean isPrimitiveOrWrapper(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() || 
               clazz == Boolean.class || 
               clazz == Byte.class || 
               clazz == Character.class || 
               clazz == Short.class || 
               clazz == Integer.class || 
               clazz == Long.class || 
               clazz == Float.class || 
               clazz == Double.class;
    }

    /**
     * 处理 JavaBean 对象
     * 将对象转为 Map，替换变量后再转回对象
     */
    @SuppressWarnings("unchecked")
    private static Object resolveJavaBean(Object bean, ExecutionContext context) {
        try {
            // 1. 将 JavaBean 转为 Map
            Map<String, Object> beanMap = objectMapper.convertValue(bean, Map.class);
            
            // 2. 递归替换 Map 中的变量
            Map<String, Object> resolvedMap = resolveMap(beanMap, context);
            
            // 3. 将 Map 转回原对象类型
            return objectMapper.convertValue(resolvedMap, bean.getClass());
        } catch (Exception e) {
            log.error("JavaBean 变量替换失败: {}", bean.getClass().getName(), e);
            return bean;
        }
    }

    /**
     * 替换字符串中的变量
     */
    private static String resolveString(String value, ExecutionContext context) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variablePath = matcher.group(1);
            Object variableValue = getVariableValue(variablePath, context);
            
            // 将变量值转换为字符串
            String replacement = variableValue != null ? String.valueOf(variableValue) : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 替换Map中的变量
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveMap(Map<?, ?> map, ExecutionContext context) {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = resolveVariables(entry.getValue(), context);
            result.put(key, value);
        }
        
        return result;
    }

    /**
     * 替换List中的变量
     */
    private static List<Object> resolveList(List<?> list, ExecutionContext context) {
        List<Object> result = new ArrayList<>();
        
        for (Object item : list) {
            result.add(resolveVariables(item, context));
        }
        
        return result;
    }

    /**
     * 获取变量值
     * 支持以下格式：
     * - {input.param}: 从输入参数中获取
     * - {node_id}: 获取节点的完整输出
     * - {node_id.field}: 获取节点输出的特定字段
     */
    private static Object getVariableValue(String variablePath, ExecutionContext context) {
        if (variablePath == null || variablePath.isEmpty()) {
            return null;
        }

        String[] parts = variablePath.split("\\.", 2);
        String firstPart = parts[0];

        if ("input".equals(firstPart)) {
            // 从输入参数中获取
            if (parts.length == 1) {
                return context.getInput();
            } else {
                String fieldName = parts[1];
                return getFieldValue(context.getInput(), fieldName);
            }
        } else {
            // 从节点输出中获取
            Object nodeOutput = context.getNodeOutput(firstPart);
            
            if (nodeOutput == null) {
                log.warn("节点 {} 的输出为空", firstPart);
                return null;
            }
            
            if (parts.length == 1) {
                // 返回整个节点输出
                return nodeOutput;
            } else {
                // 返回节点输出的特定字段
                String fieldName = parts[1];
                return getFieldValue(nodeOutput, fieldName);
            }
        }
    }

    /**
     * 从对象中获取字段值
     * 支持嵌套字段，例如: user.name, data.items.0
     */
    @SuppressWarnings("unchecked")
    private static Object getFieldValue(Object obj, String fieldPath) {
        if (obj == null || fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }

        String[] fields = fieldPath.split("\\.");
        Object current = obj;

        for (String field : fields) {
            if (current == null) {
                return null;
            }

            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(field);
            } else {
                // 尝试通过反射获取字段值
                try {
                    current = getFieldByReflection(current, field);
                } catch (Exception e) {
                    log.warn("无法获取字段 {} 的值: {}", field, e.getMessage());
                    return null;
                }
            }
        }

        return current;
    }

    /**
     * 通过反射获取对象的字段值
     */
    private static Object getFieldByReflection(Object obj, String fieldName) throws Exception {
        if (obj == null) {
            return null;
        }

        Class<?> clazz = obj.getClass();
        
        try {
            // 尝试使用getter方法
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            return clazz.getMethod(getterName).invoke(obj);
        } catch (NoSuchMethodException e) {
            // 尝试直接访问字段
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException ex) {
                throw new Exception("字段不存在: " + fieldName);
            }
        }
    }

    /**
     * 检查字符串中是否包含变量
     */
    public static boolean containsVariables(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return VARIABLE_PATTERN.matcher(value).find();
    }

    /**
     * 提取字符串中的所有变量路径
     */
    public static List<String> extractVariables(String value) {
        List<String> variables = new ArrayList<>();
        
        if (value == null || value.isEmpty()) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(value);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        
        return variables;
    }
}
