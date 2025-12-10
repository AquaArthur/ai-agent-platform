package org.demo.core.plugin.executor;

import org.demo.core.model.entity.Plugin;
import org.demo.core.model.entity.PluginOperation;
import org.demo.core.model.vo.PluginInvokeResult;

import java.util.Map;

/**
 * 插件执行器接口
 * 使用策略模式，支持不同类型的插件执行方式
 * 
 * 可扩展的执行器类型：
 * - HTTP/REST: 标准 HTTP 请求
 * - gRPC: gRPC 远程调用
 * - WebSocket: WebSocket 通信
 * - Local: 本地方法调用
 * - MessageQueue: 消息队列方式
 */
public interface PluginExecutor {

    /**
     * 判断是否支持该插件类型
     * 
     * @param pluginType 插件类型（如 rest, http, grpc, websocket 等）
     * @return 是否支持
     */
    boolean supports(String pluginType);

    /**
     * 获取执行器类型名称
     * 
     * @return 类型名称
     */
    String getType();

    /**
     * 执行插件操作
     * 
     * @param plugin    插件信息
     * @param operation 操作信息
     * @param params    调用参数
     * @param timeout   超时时间（毫秒）
     * @return 执行结果
     */
    PluginInvokeResult execute(Plugin plugin, PluginOperation operation,
            Map<String, Object> params, int timeout);
}
