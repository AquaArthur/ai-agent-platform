package org.demo.core.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 函数名映射工具类
 * 统一管理 Function Calling 中函数名与 pluginId/operationId 的映射关系
 * 
 * 命名规则: {pluginId}__{operationId}
 * 使用双下划线分隔，确保唯一性
 */
@Slf4j
public class FunctionNameMapper {

    /**
     * 函数名分隔符
     */
    public static final String SEPARATOR = "__";

    /**
     * 函数名解析结果
     */
    public static class FunctionNameParts {
        private final String pluginId;
        private final String operationId;
        private final boolean valid;

        public FunctionNameParts(String pluginId, String operationId, boolean valid) {
            this.pluginId = pluginId;
            this.operationId = operationId;
            this.valid = valid;
        }

        public String getPluginId() {
            return pluginId;
        }

        public String getOperationId() {
            return operationId;
        }

        public boolean isValid() {
            return valid;
        }

        /**
         * 创建无效的解析结果
         */
        public static FunctionNameParts invalid() {
            return new FunctionNameParts(null, null, false);
        }

        /**
         * 创建有效的解析结果
         */
        public static FunctionNameParts of(String pluginId, String operationId) {
            return new FunctionNameParts(pluginId, operationId, true);
        }
    }

    /**
     * 将 pluginId 和 operationId 编码为函数名
     * 
     * @param pluginId    插件ID
     * @param operationId 操作ID
     * @return 编码后的函数名
     */
    public static String encode(String pluginId, String operationId) {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new IllegalArgumentException("pluginId 不能为空");
        }
        if (operationId == null || operationId.isEmpty()) {
            throw new IllegalArgumentException("operationId 不能为空");
        }

        // 检查ID中是否包含分隔符
        if (pluginId.contains(SEPARATOR) || operationId.contains(SEPARATOR)) {
            log.warn("pluginId 或 operationId 中包含分隔符 '{}', 可能导致解析问题", SEPARATOR);
        }

        return pluginId + SEPARATOR + operationId;
    }

    /**
     * 从函数名解码出 pluginId 和 operationId
     * 
     * @param functionName 函数名
     * @return 解析结果（包含 pluginId、operationId 和是否有效的标志）
     */
    public static FunctionNameParts decode(String functionName) {
        if (functionName == null || functionName.isEmpty()) {
            log.warn("函数名为空");
            return FunctionNameParts.invalid();
        }

        int separatorIndex = functionName.indexOf(SEPARATOR);
        if (separatorIndex == -1) {
            log.warn("函数名格式无效，缺少分隔符: {}", functionName);
            return FunctionNameParts.invalid();
        }

        String pluginId = functionName.substring(0, separatorIndex);
        String operationId = functionName.substring(separatorIndex + SEPARATOR.length());

        if (pluginId.isEmpty() || operationId.isEmpty()) {
            log.warn("函数名格式无效，pluginId 或 operationId 为空: {}", functionName);
            return FunctionNameParts.invalid();
        }

        return FunctionNameParts.of(pluginId, operationId);
    }

    /**
     * 检查函数名格式是否有效
     * 
     * @param functionName 函数名
     * @return 是否有效
     */
    public static boolean isValid(String functionName) {
        return decode(functionName).isValid();
    }

    /**
     * 从函数名中提取 pluginId
     * 
     * @param functionName 函数名
     * @return pluginId，如果格式无效则返回 null
     */
    public static String extractPluginId(String functionName) {
        FunctionNameParts parts = decode(functionName);
        return parts.isValid() ? parts.getPluginId() : null;
    }

    /**
     * 从函数名中提取 operationId
     * 
     * @param functionName 函数名
     * @return operationId，如果格式无效则返回 null
     */
    public static String extractOperationId(String functionName) {
        FunctionNameParts parts = decode(functionName);
        return parts.isValid() ? parts.getOperationId() : null;
    }
}
