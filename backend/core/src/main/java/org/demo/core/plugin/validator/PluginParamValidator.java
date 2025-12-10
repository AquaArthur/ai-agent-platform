package org.demo.core.plugin.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 插件参数验证器
 * 根据 inputSchema（JSON Schema）验证传入的参数
 */
@Slf4j
@Component
public class PluginParamValidator {

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }

        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failure(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new ValidationResult(false, errors);
        }
    }

    /**
     * 验证参数是否符合 schema 定义
     * 
     * @param params      传入的参数
     * @param inputSchema JSON Schema 格式的输入定义
     * @return 验证结果
     */
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Map<String, Object> params, Map<String, Object> inputSchema) {
        if (inputSchema == null || inputSchema.isEmpty()) {
            // 没有 schema 定义，跳过验证
            return ValidationResult.success();
        }

        List<String> errors = new ArrayList<>();

        // 1. 检查必填字段
        Object requiredObj = inputSchema.get("required");
        if (requiredObj instanceof List) {
            List<String> requiredFields = (List<String>) requiredObj;
            for (String field : requiredFields) {
                if (params == null || !params.containsKey(field) || params.get(field) == null) {
                    errors.add("缺少必填参数: " + field);
                }
            }
        }

        // 2. 检查参数类型
        Object propertiesObj = inputSchema.get("properties");
        if (propertiesObj instanceof Map && params != null) {
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();

                if (paramValue == null) {
                    continue;
                }

                Object propDefObj = properties.get(paramName);
                if (propDefObj instanceof Map) {
                    Map<String, Object> propDef = (Map<String, Object>) propDefObj;
                    String expectedType = (String) propDef.get("type");

                    if (expectedType != null) {
                        String typeError = validateType(paramName, paramValue, expectedType);
                        if (typeError != null) {
                            errors.add(typeError);
                        }
                    }

                    // 检查枚举值
                    Object enumObj = propDef.get("enum");
                    if (enumObj instanceof List) {
                        List<?> enumValues = (List<?>) enumObj;
                        if (!enumValues.contains(paramValue)) {
                            errors.add("参数 '" + paramName + "' 的值必须是以下之一: " + enumValues);
                        }
                    }

                    // 检查最小/最大值（数值类型）
                    if (paramValue instanceof Number) {
                        Number numValue = (Number) paramValue;
                        Object minObj = propDef.get("minimum");
                        Object maxObj = propDef.get("maximum");

                        if (minObj instanceof Number) {
                            if (numValue.doubleValue() < ((Number) minObj).doubleValue()) {
                                errors.add("参数 '" + paramName + "' 的值不能小于 " + minObj);
                            }
                        }
                        if (maxObj instanceof Number) {
                            if (numValue.doubleValue() > ((Number) maxObj).doubleValue()) {
                                errors.add("参数 '" + paramName + "' 的值不能大于 " + maxObj);
                            }
                        }
                    }

                    // 检查字符串长度
                    if (paramValue instanceof String) {
                        String strValue = (String) paramValue;
                        Object minLenObj = propDef.get("minLength");
                        Object maxLenObj = propDef.get("maxLength");

                        if (minLenObj instanceof Number) {
                            if (strValue.length() < ((Number) minLenObj).intValue()) {
                                errors.add("参数 '" + paramName + "' 的长度不能少于 " + minLenObj + " 个字符");
                            }
                        }
                        if (maxLenObj instanceof Number) {
                            if (strValue.length() > ((Number) maxLenObj).intValue()) {
                                errors.add("参数 '" + paramName + "' 的长度不能超过 " + maxLenObj + " 个字符");
                            }
                        }

                        // 检查正则表达式模式
                        Object patternObj = propDef.get("pattern");
                        if (patternObj instanceof String) {
                            String pattern = (String) patternObj;
                            if (!strValue.matches(pattern)) {
                                errors.add("参数 '" + paramName + "' 的格式不正确");
                            }
                        }
                    }
                }
            }
        }

        // 3. 检查额外属性（如果 additionalProperties = false）
        Object additionalPropsObj = inputSchema.get("additionalProperties");
        if (Boolean.FALSE.equals(additionalPropsObj) && params != null) {
            Object propertiesObjForCheck = inputSchema.get("properties");
            if (propertiesObjForCheck instanceof Map) {
                Map<String, Object> properties = (Map<String, Object>) propertiesObjForCheck;
                for (String paramName : params.keySet()) {
                    if (!properties.containsKey(paramName)) {
                        errors.add("未知参数: " + paramName);
                    }
                }
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        } else {
            log.warn("参数验证失败: {}", errors);
            return ValidationResult.failure(errors);
        }
    }

    /**
     * 验证参数类型
     */
    private String validateType(String paramName, Object value, String expectedType) {
        boolean typeMatch = switch (expectedType.toLowerCase()) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Integer || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List;
            case "object" -> value instanceof Map;
            default -> true; // 未知类型，跳过验证
        };

        if (!typeMatch) {
            String actualType = value.getClass().getSimpleName();
            return "参数 '" + paramName + "' 类型错误，期望 " + expectedType + "，实际为 " + actualType;
        }
        return null;
    }
}
