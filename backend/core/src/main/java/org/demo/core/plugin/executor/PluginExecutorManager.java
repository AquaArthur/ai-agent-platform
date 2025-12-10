package org.demo.core.plugin.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.Plugin;
import org.demo.core.model.entity.PluginOperation;
import org.demo.core.model.vo.PluginInvokeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 插件执行器管理器
 * 负责选择合适的执行器来执行插件调用
 * 
 * 使用策略模式，自动根据插件类型选择对应的执行器
 */
@Slf4j
@Component
public class PluginExecutorManager {

    private final List<PluginExecutor> executors;

    @Autowired
    public PluginExecutorManager(List<PluginExecutor> executors) {
        this.executors = executors;
        log.info("初始化插件执行器管理器，已注册 {} 个执行器: {}",
                executors.size(),
                executors.stream().map(PluginExecutor::getType).toList());
    }

    /**
     * 执行插件操作
     * 自动选择合适的执行器
     * 
     * @param plugin    插件信息
     * @param operation 操作信息
     * @param params    调用参数
     * @param timeout   超时时间（毫秒）
     * @return 执行结果
     */
    public PluginInvokeResult execute(Plugin plugin, PluginOperation operation,
            Map<String, Object> params, int timeout) {
        // 获取插件类型
        String pluginType = extractPluginType(plugin);

        // 查找支持该类型的执行器
        PluginExecutor executor = findExecutor(pluginType);

        if (executor == null) {
            log.error("找不到支持类型 '{}' 的执行器", pluginType);
            return PluginInvokeResult.error(
                    "不支持的插件类型: " + (pluginType != null ? pluginType : "未知"),
                    0);
        }

        log.debug("使用 {} 执行器执行插件 {} 的操作 {}",
                executor.getType(), plugin.getId(), operation.getOperationId());

        return executor.execute(plugin, operation, params, timeout);
    }

    /**
     * 从插件配置中提取插件类型
     */
    @SuppressWarnings("unchecked")
    private String extractPluginType(Plugin plugin) {
        Object openapiSpec = plugin.getOpenapiSpec();
        if (openapiSpec instanceof Map) {
            Object type = ((Map<String, Object>) openapiSpec).get("type");
            if (type instanceof String) {
                return (String) type;
            }
        }
        // 默认类型
        return "rest";
    }

    /**
     * 查找支持指定类型的执行器
     */
    private PluginExecutor findExecutor(String pluginType) {
        for (PluginExecutor executor : executors) {
            if (executor.supports(pluginType)) {
                return executor;
            }
        }
        return null;
    }

    /**
     * 获取所有已注册的执行器类型
     */
    public List<String> getRegisteredTypes() {
        return executors.stream()
                .map(PluginExecutor::getType)
                .toList();
    }

    /**
     * 检查是否支持指定的插件类型
     */
    public boolean isSupported(String pluginType) {
        return findExecutor(pluginType) != null;
    }
}
