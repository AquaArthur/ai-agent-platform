package org.demo.core.workflow.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.Workflow;
import org.demo.core.workflow.node.BaseNodeConfig;
import org.demo.core.workflow.node.HttpNodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求节点执行器
 * 调用外部HTTP API服务
 */
@Slf4j
@Component
public class HttpNodeExecutor implements NodeExecutor {

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) throws Exception {
        log.info("执行HTTP节点: {}", node.getId());
        
        HttpNodeConfig httpConfig = (HttpNodeConfig) config;
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        if (httpConfig.getHeaders() != null) {
            httpConfig.getHeaders().forEach(headers::add);
        }
        
        // 构建请求
        HttpEntity<?> requestEntity;
        if ("POST".equalsIgnoreCase(httpConfig.getMethod()) && httpConfig.getBody() != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            requestEntity = new HttpEntity<>(httpConfig.getBody(), headers);
        } else {
            requestEntity = new HttpEntity<>(headers);
        }
        
        // 发送请求
        HttpMethod method = "POST".equalsIgnoreCase(httpConfig.getMethod()) ? HttpMethod.POST : HttpMethod.GET;
        ResponseEntity<String> response = restTemplate.exchange(
            httpConfig.getUrl(),
            method,
            requestEntity,
            String.class
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.getStatusCodeValue());
        result.put("output", response.getBody());
        result.put("headers", response.getHeaders().toSingleValueMap());
        
        return result;
    }

    @Override
    public String getSupportedType() {
        return "http";
    }
}
