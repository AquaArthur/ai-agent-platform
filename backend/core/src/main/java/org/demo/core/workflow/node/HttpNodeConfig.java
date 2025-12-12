package org.demo.core.workflow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * HTTP请求节点配置
 * 调用外部HTTP API服务
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HttpNodeConfig extends BaseNodeConfig {
    /**
     * 请求URL，支持变量替换（必填）
     */
    private String url;

    /**
     * 请求方法，支持GET/POST，默认GET
     */
    private String method = "GET";

    /**
     * 请求头，支持变量替换
     */
    private Map<String, String> headers;

    /**
     * 请求体（POST请求时使用），支持变量替换
     */
    private Object body;

    public HttpNodeConfig() {
        setType("http");
    }

    @Override
    public void validate() {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP节点的 url 不能为空");
        }
        if (method != null && !method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("POST")) {
            throw new IllegalArgumentException("HTTP节点的 method 只支持 GET 或 POST");
        }
    }
}
