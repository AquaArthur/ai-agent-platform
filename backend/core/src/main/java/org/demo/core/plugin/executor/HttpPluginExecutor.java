package org.demo.core.plugin.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.Plugin;
import org.demo.core.model.entity.PluginOperation;
import org.demo.core.model.vo.PluginInvokeResult;
import org.demo.core.plugin.validator.PluginParamValidator;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * HTTP/REST 插件执行器
 * 负责执行 HTTP REST 类型的插件调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpPluginExecutor implements PluginExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PluginParamValidator paramValidator;

    @Override
    public boolean supports(String pluginType) {
        if (pluginType == null || pluginType.isEmpty()) {
            // 默认支持空类型（兼容旧数据）
            return true;
        }
        String type = pluginType.toLowerCase();
        // 支持 rest, http, https, openapi 类型（OpenAPI 本质上也是 HTTP REST 调用）
        return "rest".equals(type) || "http".equals(type) || "https".equals(type) || "openapi".equals(type);
    }

    @Override
    public String getType() {
        return "HTTP";
    }

    @Override
    public PluginInvokeResult execute(Plugin plugin, PluginOperation operation,
            Map<String, Object> params, int timeout) {
        long startTime = System.currentTimeMillis();

        try {
            // 0. 参数验证
            PluginParamValidator.ValidationResult validationResult = paramValidator.validate(params,
                    operation.getInputSchema());
            if (!validationResult.isValid()) {
                return PluginInvokeResult.error(
                        "参数验证失败: " + validationResult.getErrorMessage(),
                        System.currentTimeMillis() - startTime);
            }

            // 1. 获取 baseUrl
            String baseUrl = extractBaseUrl(plugin);
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                return PluginInvokeResult.error("插件未配置baseUrl", System.currentTimeMillis() - startTime);
            }

            // 2. 构建请求URL
            String fullUrl = buildRequestUrl(baseUrl, operation.getPath(), operation.getMethod(), params);

            // 3. 构建请求头（含鉴权信息）
            HttpHeaders headers = buildHttpHeaders(plugin.getAuthType(), parseAuthConfig(plugin.getAuthConfig()));

            // 4. 构建请求体
            HttpEntity<?> requestEntity = buildRequestEntity(operation.getMethod(), params, headers);

            // 5. 发送HTTP请求
            HttpMethod httpMethod = HttpMethod.valueOf(operation.getMethod().toUpperCase());

            log.debug("执行HTTP请求: {} {}", httpMethod, fullUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    httpMethod,
                    requestEntity,
                    String.class);

            long duration = System.currentTimeMillis() - startTime;

            // 6. 解析响应
            String rawBody = response.getBody();
            Object parsedData = parseResponseBody(rawBody);

            PluginInvokeResult result = PluginInvokeResult.success(
                    response.getStatusCode().value(),
                    rawBody,
                    parsedData,
                    duration);
            result.setRequestUrl(fullUrl);
            result.setRequestMethod(operation.getMethod());

            log.info("HTTP请求成功: {} {} -> {} ({}ms)",
                    httpMethod, fullUrl, response.getStatusCode(), duration);

            return result;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // HTTP 4xx/5xx 错误
            long duration = System.currentTimeMillis() - startTime;
            log.warn("HTTP错误: {} - {}", e.getStatusCode(), e.getStatusText());

            PluginInvokeResult result = PluginInvokeResult.error(
                    "HTTP错误: " + e.getStatusCode() + " - " + e.getStatusText(),
                    duration);
            result.setHttpStatusCode(e.getStatusCode().value());
            result.setRawBody(e.getResponseBodyAsString());
            return result;

        } catch (ResourceAccessException e) {
            // 连接超时或网络错误
            long duration = System.currentTimeMillis() - startTime;
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("HTTP请求超时");
                return PluginInvokeResult.timeout("请求超时", duration);
            }
            log.warn("网络错误: {}", e.getMessage());
            return PluginInvokeResult.error("网络错误: " + e.getMessage(), duration);

        } catch (Exception e) {
            log.error("HTTP执行失败", e);
            return PluginInvokeResult.error("调用失败: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 从插件配置中提取 baseUrl
     */
    @SuppressWarnings("unchecked")
    private String extractBaseUrl(Plugin plugin) {
        Object openapiSpec = plugin.getOpenapiSpec();
        if (openapiSpec instanceof Map) {
            return (String) ((Map<String, Object>) openapiSpec).get("baseUrl");
        }
        return null;
    }

    /**
     * 解析 authConfig 字段
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAuthConfig(Object authConfig) {
        if (authConfig == null) {
            return null;
        }
        if (authConfig instanceof Map) {
            return (Map<String, Object>) authConfig;
        }
        if (authConfig instanceof String) {
            try {
                return objectMapper.readValue((String) authConfig,
                        new TypeReference<Map<String, Object>>() {
                        });
            } catch (Exception e) {
                log.warn("解析authConfig失败: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * 解析响应体
     */
    private Object parseResponseBody(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawBody, Object.class);
        } catch (Exception e) {
            // 如果解析失败，保留原始字符串
            return rawBody;
        }
    }

    /**
     * 构建请求URL
     */
    private String buildRequestUrl(String baseUrl, String path, String method, Map<String, Object> params) {
        // 去除baseUrl末尾的斜杠
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        // 确保path以斜杠开头
        if (path != null && !path.startsWith("/")) {
            path = "/" + path;
        }

        String fullUrl = baseUrl + (path != null ? path : "");

        // 如果是GET请求，将参数拼接到URL
        if ("GET".equalsIgnoreCase(method) && params != null && !params.isEmpty()) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(fullUrl);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getValue() != null) {
                    builder.queryParam(entry.getKey(), entry.getValue().toString());
                }
            }
            fullUrl = builder.toUriString();
        }

        return fullUrl;
    }

    /**
     * 构建HTTP请求头（含鉴权信息）
     */
    private HttpHeaders buildHttpHeaders(String authType, Map<String, Object> authConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (authType == null || "none".equalsIgnoreCase(authType)) {
            return headers;
        }

        if (authConfig == null) {
            return headers;
        }

        switch (authType.toLowerCase()) {
            case "api_key":
                // API Key 认证
                String apiKey = (String) authConfig.get("apiKey");
                String headerName = (String) authConfig.getOrDefault("headerName", "X-API-Key");
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.set(headerName, apiKey);
                }
                break;

            case "bearer":
                // Bearer Token 认证
                String token = (String) authConfig.get("token");
                if (token != null && !token.isEmpty()) {
                    headers.setBearerAuth(token);
                }
                break;

            case "basic":
                // Basic Auth 认证
                String username = (String) authConfig.get("username");
                String password = (String) authConfig.get("password");
                if (username != null && password != null) {
                    headers.setBasicAuth(username, password);
                }
                break;

            case "custom":
                // 自定义Header
                Object customHeaders = authConfig.get("headers");
                if (customHeaders instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> customHeaderMap = (Map<String, String>) customHeaders;
                    customHeaderMap.forEach(headers::set);
                }
                break;
        }

        return headers;
    }

    /**
     * 构建请求实体
     */
    private HttpEntity<?> buildRequestEntity(String method, Map<String, Object> params, HttpHeaders headers) {
        // GET请求不需要body
        if ("GET".equalsIgnoreCase(method)) {
            return new HttpEntity<>(headers);
        }

        // POST/PUT/PATCH请求将参数放到body
        if (params != null && !params.isEmpty()) {
            return new HttpEntity<>(params, headers);
        }

        return new HttpEntity<>(headers);
    }
}
