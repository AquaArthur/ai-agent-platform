package org.demo.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.core.exception.PluginNotFoundException;
import org.demo.core.mapper.PluginMapper;
import org.demo.core.model.dto.PluginCreateRequest;
import org.demo.core.model.dto.PluginOpenApiImportRequest;
import org.demo.core.model.dto.PluginUpdateRequest;
import org.demo.core.model.entity.Plugin;
import org.demo.core.model.entity.PluginOperation;
import org.demo.core.model.vo.PageResult;
import org.demo.core.model.vo.PluginDetailVO;
import org.demo.core.model.vo.PluginListItemVO;
import org.demo.core.model.vo.PluginOperationVO;
import org.demo.core.model.vo.PluginInvokeResult;
import org.demo.core.plugin.executor.PluginExecutorManager;
import org.demo.core.service.PluginOperationService;
import org.demo.core.service.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 插件服务实现类
 */
@Service
public class PluginServiceImpl implements PluginService {

    /**
     * 默认超时时间（毫秒），可通过配置文件修改
     */
    @Value("${plugin.invoke.timeout:30000}")
    private int defaultTimeout;

    @Autowired
    private PluginMapper pluginMapper;

    @Autowired
    private PluginOperationService pluginOperationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PluginExecutorManager pluginExecutorManager;

    @Override
    public PageResult<PluginListItemVO> listPlugins(Integer pageNo, Integer pageSize) {
        // 默认值处理
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 分页查询
        Page<Plugin> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Plugin> queryWrapper = new LambdaQueryWrapper<>();
        
        // 权限过滤：只能看到系统插件（user_id为null）或自己创建的插件
        // 注意：这里暂时返回所有插件，因为方法签名中没有 userId 参数
        // 建议后续修改接口签名添加 userId 参数
        
        queryWrapper.orderByDesc(Plugin::getCreateTime);

        Page<Plugin> resultPage = pluginMapper.selectPage(pageParam, queryWrapper);

        // 转换为VO
        List<PluginListItemVO> list = resultPage.getRecords().stream()
                .map(this::convertToListItemVO)
                .collect(Collectors.toList());

        return PageResult.of(list, resultPage.getTotal());
    }

    @Override
    public PluginDetailVO getPluginDetail(String pluginId) {
        Plugin plugin = pluginMapper.selectById(pluginId);
        if (plugin == null) {
            throw new PluginNotFoundException(pluginId);
        }

        // 权限检查：系统插件（user_id为null）可以被所有人访问
        // 用户创建的插件暂时也允许所有人访问（可根据需求调整）
        // TODO: 如需严格权限控制，可以添加 userId 参数并检查 plugin.getUserId().equals(userId)

        // 获取插件的所有操作
        List<PluginOperation> operations = pluginOperationService.listByPluginId(pluginId);

        return convertToDetailVO(plugin, operations);
    }

    @Override
    @Transactional
    public PluginDetailVO createPlugin(PluginCreateRequest request, String userId) {
        // 参数校验
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("插件名称不能为空");
        }

        Plugin plugin = new Plugin();

        // 生成ID: plugin_ + UUID简写
        String id = "plugin_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        plugin.setId(id);

        // 设置基本信息
        plugin.setName(request.getName());
        plugin.setDescription(request.getDescription());
        plugin.setIdentifier(generateIdentifier(request.getName()));

        // 构建openapi_spec存储method, endpoint, paramsSchema, responseSchema
        Map<String, Object> openapiSpec = new HashMap<>();
        openapiSpec.put("method", request.getMethod());
        openapiSpec.put("endpoint", request.getEndpoint());
        openapiSpec.put("paramsSchema", request.getParamsSchema());
        openapiSpec.put("responseSchema", request.getResponseSchema());
        plugin.setOpenapiSpec(openapiSpec);

        // 设置鉴权信息
        plugin.setAuthType(request.getAuthType() != null ? request.getAuthType() : "none");
        plugin.setAuthConfig(request.getAuthConfig());

        // 设置默认值
        plugin.setStatus("enabled");
        plugin.setIsEnabled(true);
        plugin.setUserId(userId);

        // 设置时间
        LocalDateTime now = LocalDateTime.now();
        plugin.setCreateTime(now);
        plugin.setUpdateTime(now);

        pluginMapper.insert(plugin);

        return convertToDetailVO(plugin, new ArrayList<>());
    }

    @Override
    @Transactional
    public PluginDetailVO updatePlugin(String pluginId, PluginUpdateRequest request, String userId) {
        Plugin plugin = pluginMapper.selectById(pluginId);
        if (plugin == null) {
            throw new PluginNotFoundException(pluginId);
        }

        // 权限检查：只有创建者可以修改自己的插件
        if (plugin.getUserId() == null) {
            throw new SecurityException("系统插件不允许修改");
        }
        if (!plugin.getUserId().equals(userId)) {
            throw new SecurityException("无权修改该插件");
        }

        // 更新基本信息（只更新非空字段）
        if (request.getName() != null) {
            plugin.setName(request.getName());
        }
        if (request.getDescription() != null) {
            plugin.setDescription(request.getDescription());
        }

        // 更新openapi_spec中的字段
        Map<String, Object> openapiSpec = parseOpenapiSpec(plugin.getOpenapiSpec());
        if (openapiSpec == null) {
            openapiSpec = new HashMap<>();
        }
        if (request.getMethod() != null) {
            openapiSpec.put("method", request.getMethod());
        }
        if (request.getEndpoint() != null) {
            openapiSpec.put("endpoint", request.getEndpoint());
        }
        if (request.getParamsSchema() != null) {
            openapiSpec.put("paramsSchema", request.getParamsSchema());
        }
        if (request.getResponseSchema() != null) {
            openapiSpec.put("responseSchema", request.getResponseSchema());
        }
        plugin.setOpenapiSpec(openapiSpec);

        // 更新鉴权信息
        if (request.getAuthType() != null) {
            plugin.setAuthType(request.getAuthType());
        }
        if (request.getAuthConfig() != null) {
            plugin.setAuthConfig(request.getAuthConfig());
        }

        // 更新时间
        plugin.setUpdateTime(LocalDateTime.now());

        pluginMapper.updateById(plugin);

        // 获取更新后的操作列表
        List<PluginOperation> operations = pluginOperationService.listByPluginId(pluginId);
        return convertToDetailVO(plugin, operations);
    }

    @Override
    @Transactional
    public void deletePlugin(String pluginId, String userId) {
        Plugin plugin = pluginMapper.selectById(pluginId);
        if (plugin == null) {
            throw new PluginNotFoundException(pluginId);
        }
        
        // 权限检查：只有创建者可以删除自己的插件
        if (plugin.getUserId() == null) {
            throw new SecurityException("系统插件不允许删除");
        }
        if (!plugin.getUserId().equals(userId)) {
            throw new SecurityException("无权删除该插件");
        }
        
        // 先删除关联的操作
        pluginOperationService.deleteByPluginId(pluginId);
        // 再删除插件
        pluginMapper.deleteById(pluginId);
    }

    /**
     * 生成插件标识符
     */
    private String generateIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return "plugin_" + System.currentTimeMillis();
        }
        // 转小写，空格替换为下划线，移除特殊字符
        return name.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    /**
     * 转换为列表项VO
     */
    private PluginListItemVO convertToListItemVO(Plugin plugin) {
        PluginListItemVO vo = new PluginListItemVO();
        vo.setId(plugin.getId());
        vo.setName(plugin.getName());
        vo.setDescription(plugin.getDescription());
        vo.setIsEnabled(plugin.getIsEnabled());
        vo.setCreateTime(toTimestamp(plugin.getCreateTime()));
        vo.setUpdateTime(toTimestamp(plugin.getUpdateTime()));
        return vo;
    }

    /**
     * 转换为详情VO（带operations）
     */
    private PluginDetailVO convertToDetailVO(Plugin plugin, List<PluginOperation> operations) {
        PluginDetailVO vo = new PluginDetailVO();
        vo.setId(plugin.getId());
        vo.setName(plugin.getName());
        vo.setDescription(plugin.getDescription());
        vo.setAuthType(plugin.getAuthType());
        vo.setAuthConfig(maskSensitiveInfo(parseAuthConfig(plugin.getAuthConfig())));
        vo.setCreateTime(toTimestamp(plugin.getCreateTime()));
        vo.setUpdateTime(toTimestamp(plugin.getUpdateTime()));

        // 设置状态字段
        vo.setStatus(plugin.getStatus());
        vo.setIsEnabled(plugin.getIsEnabled());

        // 从openapi_spec中提取字段（type, baseUrl 等）
        Map<String, Object> openapiSpec = parseOpenapiSpec(plugin.getOpenapiSpec());
        if (openapiSpec != null) {
            vo.setType((String) openapiSpec.get("type"));
            vo.setBaseUrl((String) openapiSpec.get("baseUrl"));

            // 兼容旧数据：从openapi_spec中读取method, endpoint等（如果operations为空）
            if (operations == null || operations.isEmpty()) {
                vo.setMethod((String) openapiSpec.get("method"));
                vo.setEndpoint((String) openapiSpec.get("endpoint"));

                Object paramsSchema = openapiSpec.get("paramsSchema");
                if (paramsSchema instanceof Map) {
                    vo.setParamsSchema((Map<String, Object>) paramsSchema);
                }

                Object responseSchema = openapiSpec.get("responseSchema");
                if (responseSchema instanceof Map) {
                    vo.setResponseSchema((Map<String, Object>) responseSchema);
                }
            }
        }

        // 转换operations
        if (operations != null && !operations.isEmpty()) {
            List<PluginOperationVO> operationVOs = operations.stream()
                    .map(this::convertToOperationVO)
                    .collect(Collectors.toList());
            vo.setOperations(operationVOs);

            // 使用第一个operation填充兼容字段
            PluginOperation firstOp = operations.get(0);
            vo.setMethod(firstOp.getMethod());
            vo.setEndpoint(firstOp.getPath());
            vo.setParamsSchema(firstOp.getInputSchema());
            vo.setResponseSchema(firstOp.getOutputSchema());
        } else {
            vo.setOperations(new ArrayList<>());
        }

        return vo;
    }

    /**
     * 转换PluginOperation为VO
     */
    private PluginOperationVO convertToOperationVO(PluginOperation operation) {
        PluginOperationVO vo = new PluginOperationVO();
        vo.setId(operation.getId());
        vo.setOperationId(operation.getOperationId());
        vo.setName(operation.getName());
        vo.setMethod(operation.getMethod());
        vo.setPath(operation.getPath());
        vo.setDescription(operation.getDescription());
        vo.setInputSchema(operation.getInputSchema());
        vo.setOutputSchema(operation.getOutputSchema());
        return vo;
    }

    /**
     * 解析openapi_spec字段
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenapiSpec(Object openapiSpec) {
        if (openapiSpec == null) {
            return null;
        }
        if (openapiSpec instanceof Map) {
            return (Map<String, Object>) openapiSpec;
        }
        if (openapiSpec instanceof String) {
            try {
                return objectMapper.readValue((String) openapiSpec,
                        new TypeReference<Map<String, Object>>() {
                        });
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 解析authConfig字段
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
                return null;
            }
        }
        return null;
    }

    /**
     * 脱敏敏感信息（如apiKey）
     */
    private Map<String, Object> maskSensitiveInfo(Map<String, Object> authConfig) {
        if (authConfig == null) {
            return null;
        }
        Map<String, Object> masked = new LinkedHashMap<>(authConfig);
        // 脱敏apiKey字段
        if (masked.containsKey("apiKey")) {
            masked.put("apiKey", "***");
        }
        return masked;
    }

    /**
     * LocalDateTime转毫秒时间戳
     */
    private Long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    @Transactional
    public PluginDetailVO importFromOpenApi(PluginOpenApiImportRequest request, String userId) {
        // 参数校验
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("插件名称不能为空");
        }

        // 1. 创建插件主记录
        Plugin plugin = new Plugin();
        String pluginId = "plugin_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        plugin.setId(pluginId);
        plugin.setName(request.getName());
        plugin.setDescription(request.getDescription());
        plugin.setIdentifier(generateIdentifier(request.getName()));

        // 构建openapi_spec存储type, baseUrl等元信息
        Map<String, Object> openapiSpec = new HashMap<>();
        openapiSpec.put("type", request.getType() != null ? request.getType() : "rest");
        openapiSpec.put("baseUrl", request.getBaseUrl());

        // 如果提供了原始OpenAPI规范，也存储它
        if (request.getOpenapiSpec() != null) {
            openapiSpec.put("originalSpec", request.getOpenapiSpec());
        }
        plugin.setOpenapiSpec(openapiSpec);

        // 设置鉴权信息
        plugin.setAuthType(request.getAuthType() != null ? request.getAuthType() : "none");
        plugin.setAuthConfig(request.getAuthConfig());

        // 设置默认值
        plugin.setStatus("enabled");
        plugin.setIsEnabled(true);
        plugin.setUserId(userId);

        // 设置时间
        LocalDateTime now = LocalDateTime.now();
        plugin.setCreateTime(now);
        plugin.setUpdateTime(now);

        pluginMapper.insert(plugin);

        // 2. 解析并创建操作记录
        List<PluginOperation> operations = new ArrayList<>();

        if (request.getOperations() != null && !request.getOperations().isEmpty()) {
            // 使用简化模式的操作定义
            for (PluginOpenApiImportRequest.OperationDefinition opDef : request.getOperations()) {
                PluginOperation operation = new PluginOperation();
                operation.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
                operation.setPluginId(pluginId);
                operation.setOperationId(opDef.getOperationId() != null ? opDef.getOperationId()
                        : generateOperationId(opDef.getMethod(), opDef.getPath()));
                operation.setName(opDef.getName() != null ? opDef.getName() : opDef.getOperationId());
                operation.setMethod(opDef.getMethod() != null ? opDef.getMethod().toUpperCase() : "GET");
                operation.setPath(opDef.getPath());
                operation.setDescription(opDef.getDescription());
                operation.setInputSchema(opDef.getInputSchema());
                operation.setOutputSchema(opDef.getOutputSchema());
                operation.setCreateTime(now);
                operation.setUpdateTime(now);
                operations.add(operation);
            }
        } else if (request.getOpenapiSpec() != null) {
            // 解析OpenAPI规范
            operations = parseOpenApiSpec(request.getOpenapiSpec(), pluginId, now);
        }

        // 批量保存操作
        if (!operations.isEmpty()) {
            pluginOperationService.saveBatch(operations);
        }

        return convertToDetailVO(plugin, operations);
    }

    @Override
    public List<PluginOperationVO> listOperations(String pluginId) {
        // 检查插件是否存在
        Plugin plugin = pluginMapper.selectById(pluginId);
        if (plugin == null) {
            throw new PluginNotFoundException(pluginId);
        }

        List<PluginOperation> operations = pluginOperationService.listByPluginId(pluginId);
        return operations.stream()
                .map(this::convertToOperationVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PluginDetailVO updatePluginStatus(String pluginId, Boolean isEnabled, String userId) {
        Plugin plugin = pluginMapper.selectById(pluginId);
        if (plugin == null) {
            throw new PluginNotFoundException(pluginId);
        }

        // 更新状态
        plugin.setIsEnabled(isEnabled);
        plugin.setStatus(isEnabled ? "enabled" : "disabled");
        plugin.setUpdateTime(LocalDateTime.now());

        pluginMapper.updateById(plugin);

        // 获取操作列表
        List<PluginOperation> operations = pluginOperationService.listByPluginId(pluginId);
        return convertToDetailVO(plugin, operations);
    }

    @Override
    public PluginInvokeResult invokeOperation(String pluginId, String operationId, Map<String, Object> params,
            Integer timeout) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取插件信息
            Plugin plugin = pluginMapper.selectById(pluginId);
            if (plugin == null) {
                throw new PluginNotFoundException(pluginId);
            }

            // 检查插件是否启用
            if (!Boolean.TRUE.equals(plugin.getIsEnabled())) {
                return PluginInvokeResult.error("插件已禁用", System.currentTimeMillis() - startTime);
            }

            // 2. 获取操作信息
            PluginOperation operation = pluginOperationService.getByPluginIdAndOperationId(pluginId, operationId);
            if (operation == null) {
                return PluginInvokeResult.error("操作不存在: " + operationId, System.currentTimeMillis() - startTime);
            }

            // 3. 使用执行器管理器执行插件调用（策略模式）
            int actualTimeout = timeout != null ? timeout : defaultTimeout;
            return pluginExecutorManager.execute(plugin, operation, params, actualTimeout);

        } catch (PluginNotFoundException e) {
            return PluginInvokeResult.error(e.getMessage(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            return PluginInvokeResult.error("调用失败: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 生成操作ID
     */
    private String generateOperationId(String method, String path) {
        if (method == null || path == null) {
            return "op_" + System.currentTimeMillis();
        }
        // 例如: GET /api/users -> get_api_users
        String cleanPath = path.replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return method.toLowerCase() + "_" + cleanPath;
    }

    /**
     * 解析OpenAPI 3.0规范，提取操作列表
     */
    @SuppressWarnings("unchecked")
    private List<PluginOperation> parseOpenApiSpec(Object openapiSpecObj, String pluginId, LocalDateTime now) {
        List<PluginOperation> operations = new ArrayList<>();

        Map<String, Object> spec = parseOpenapiSpec(openapiSpecObj);
        if (spec == null) {
            return operations;
        }

        // 获取paths对象
        Object pathsObj = spec.get("paths");
        if (!(pathsObj instanceof Map)) {
            return operations;
        }
        Map<String, Object> paths = (Map<String, Object>) pathsObj;

        // 遍历每个路径
        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            String path = pathEntry.getKey();
            Object pathItemObj = pathEntry.getValue();
            if (!(pathItemObj instanceof Map)) {
                continue;
            }
            Map<String, Object> pathItem = (Map<String, Object>) pathItemObj;

            // 遍历每个HTTP方法
            for (String method : List.of("get", "post", "put", "delete", "patch")) {
                Object operationObj = pathItem.get(method);
                if (!(operationObj instanceof Map)) {
                    continue;
                }
                Map<String, Object> operationSpec = (Map<String, Object>) operationObj;

                PluginOperation operation = new PluginOperation();
                operation.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
                operation.setPluginId(pluginId);

                // 获取operationId
                String operationId = (String) operationSpec.get("operationId");
                if (operationId == null) {
                    operationId = generateOperationId(method, path);
                }
                operation.setOperationId(operationId);

                // 获取summary作为name
                String summary = (String) operationSpec.get("summary");
                operation.setName(summary != null ? summary : operationId);

                operation.setMethod(method.toUpperCase());
                operation.setPath(path);

                // 获取description
                operation.setDescription((String) operationSpec.get("description"));

                // 解析requestBody作为inputSchema
                Object requestBody = operationSpec.get("requestBody");
                if (requestBody instanceof Map) {
                    Map<String, Object> inputSchema = extractSchemaFromRequestBody((Map<String, Object>) requestBody);
                    operation.setInputSchema(inputSchema);
                }

                // 解析responses中的200响应作为outputSchema
                Object responses = operationSpec.get("responses");
                if (responses instanceof Map) {
                    Map<String, Object> outputSchema = extractSchemaFromResponses((Map<String, Object>) responses);
                    operation.setOutputSchema(outputSchema);
                }

                operation.setCreateTime(now);
                operation.setUpdateTime(now);
                operations.add(operation);
            }
        }

        return operations;
    }

    /**
     * 从requestBody中提取schema
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSchemaFromRequestBody(Map<String, Object> requestBody) {
        Object content = requestBody.get("content");
        if (!(content instanceof Map)) {
            return null;
        }
        Map<String, Object> contentMap = (Map<String, Object>) content;

        // 优先获取application/json
        Object jsonContent = contentMap.get("application/json");
        if (jsonContent == null) {
            // 获取第一个content type
            jsonContent = contentMap.values().stream().findFirst().orElse(null);
        }

        if (!(jsonContent instanceof Map)) {
            return null;
        }
        Map<String, Object> mediaType = (Map<String, Object>) jsonContent;

        Object schema = mediaType.get("schema");
        if (schema instanceof Map) {
            return (Map<String, Object>) schema;
        }
        return null;
    }

    /**
     * 从responses中提取schema（优先200响应）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSchemaFromResponses(Map<String, Object> responses) {
        // 优先获取200响应
        Object response = responses.get("200");
        if (response == null) {
            response = responses.get("201");
        }
        if (response == null) {
            // 获取第一个成功响应
            response = responses.values().stream().findFirst().orElse(null);
        }

        if (!(response instanceof Map)) {
            return null;
        }
        Map<String, Object> responseMap = (Map<String, Object>) response;

        Object content = responseMap.get("content");
        if (!(content instanceof Map)) {
            return null;
        }
        Map<String, Object> contentMap = (Map<String, Object>) content;

        // 优先获取application/json
        Object jsonContent = contentMap.get("application/json");
        if (jsonContent == null) {
            jsonContent = contentMap.values().stream().findFirst().orElse(null);
        }

        if (!(jsonContent instanceof Map)) {
            return null;
        }
        Map<String, Object> mediaType = (Map<String, Object>) jsonContent;

        Object schema = mediaType.get("schema");
        if (schema instanceof Map) {
            return (Map<String, Object>) schema;
        }
        return null;
    }
}
