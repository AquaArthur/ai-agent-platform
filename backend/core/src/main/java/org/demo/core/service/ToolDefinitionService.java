package org.demo.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.mapper.PluginMapper;
import org.demo.core.mapper.PluginOperationMapper;
import org.demo.core.model.entity.Agent;
import org.demo.core.model.entity.Plugin;
import org.demo.core.model.entity.PluginOperation;
import org.demo.core.util.FunctionNameMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 工具定义服务
 * 负责将插件操作转换为 OpenAI Function Calling 的工具定义
 * 
 * 特性:
 * - 统一管理工具定义的构建逻辑
 * - 支持从数据库插件配置生成工具定义
 * - 提供工具名称到插件操作的映射
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolDefinitionService {

    private final PluginMapper pluginMapper;
    private final PluginOperationMapper pluginOperationMapper;

    /**
     * 工具信息
     * 包含工具定义和相关元数据
     */
    public static class ToolInfo {
        private final ChatCompletionFunctionTool tool;
        private final String pluginId;
        private final String pluginName;
        private final String operationId;
        private final String operationName;

        public ToolInfo(ChatCompletionFunctionTool tool, String pluginId, String pluginName,
                String operationId, String operationName) {
            this.tool = tool;
            this.pluginId = pluginId;
            this.pluginName = pluginName;
            this.operationId = operationId;
            this.operationName = operationName;
        }

        public ChatCompletionFunctionTool getTool() {
            return tool;
        }

        public String getPluginId() {
            return pluginId;
        }

        public String getPluginName() {
            return pluginName;
        }

        public String getOperationId() {
            return operationId;
        }

        public String getOperationName() {
            return operationName;
        }

        public String getFunctionName() {
            return FunctionNameMapper.encode(pluginId, operationId);
        }
    }

    /**
     * 根据智能体配置构建工具列表
     * 
     * @param agent 智能体
     * @return 工具信息列表
     */
    public List<ToolInfo> buildToolsFromAgent(Agent agent) {
        List<ToolInfo> toolInfoList = new ArrayList<>();

        List<String> pluginIds = agent.getToolsConfig();
        if (pluginIds == null || pluginIds.isEmpty()) {
            log.debug("智能体 {} 没有绑定插件", agent.getId());
            return toolInfoList;
        }

        log.info("智能体 {} 绑定了 {} 个插件: {}", agent.getId(), pluginIds.size(), pluginIds);

        for (String pluginId : pluginIds) {
            try {
                List<ToolInfo> pluginTools = buildToolsFromPlugin(pluginId);
                toolInfoList.addAll(pluginTools);
            } catch (Exception e) {
                log.warn("构建插件工具失败: pluginId={}, error={}", pluginId, e.getMessage());
            }
        }

        return toolInfoList;
    }

    /**
     * 根据插件ID构建工具列表
     * 
     * @param pluginId 插件ID
     * @return 工具信息列表
     */
    public List<ToolInfo> buildToolsFromPlugin(String pluginId) {
        List<ToolInfo> toolInfoList = new ArrayList<>();

        // 查询插件
        Plugin plugin = pluginMapper.selectById(pluginId);
        if (plugin == null) {
            log.warn("插件不存在: {}", pluginId);
            return toolInfoList;
        }

        if (!Boolean.TRUE.equals(plugin.getIsEnabled())) {
            log.warn("插件已禁用: {}", pluginId);
            return toolInfoList;
        }

        // 查询插件的所有操作
        LambdaQueryWrapper<PluginOperation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PluginOperation::getPluginId, pluginId);
        List<PluginOperation> operations = pluginOperationMapper.selectList(queryWrapper);

        log.info("插件 {} ({}) 有 {} 个操作", plugin.getName(), pluginId, operations.size());

        for (PluginOperation op : operations) {
            ToolInfo toolInfo = buildToolFromOperation(plugin, op);
            if (toolInfo != null) {
                toolInfoList.add(toolInfo);
                log.debug("  添加工具: {}", op.getOperationId());
            }
        }

        return toolInfoList;
    }

    /**
     * 从插件操作构建单个工具
     * 
     * @param plugin    插件
     * @param operation 操作
     * @return 工具信息，如果构建失败返回 null
     */
    public ToolInfo buildToolFromOperation(Plugin plugin, PluginOperation operation) {
        try {
            // 使用 FunctionNameMapper 统一编码函数名
            String functionName = FunctionNameMapper.encode(plugin.getId(), operation.getOperationId());

            // 构建函数描述
            String description = String.format("[%s] %s", plugin.getName(),
                    operation.getDescription() != null ? operation.getDescription() : operation.getName());

            // 构建参数 schema
            FunctionParameters parameters = buildParameters(operation.getInputSchema());

            // 构建 FunctionDefinition
            FunctionDefinition functionDef = FunctionDefinition.builder()
                    .name(functionName)
                    .description(description)
                    .parameters(parameters)
                    .build();

            // 构建 ChatCompletionFunctionTool
            ChatCompletionFunctionTool tool = ChatCompletionFunctionTool.builder()
                    .function(functionDef)
                    .build();

            return new ToolInfo(tool, plugin.getId(), plugin.getName(),
                    operation.getOperationId(), operation.getName());

        } catch (Exception e) {
            log.warn("构建工具定义失败: operationId={}, error={}", operation.getOperationId(), e.getMessage());
            return null;
        }
    }

    /**
     * 构建函数参数定义
     * 
     * @param inputSchema 输入 schema
     * @return 函数参数
     */
    private FunctionParameters buildParameters(Map<String, Object> inputSchema) {
        FunctionParameters.Builder parametersBuilder = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"));

        if (inputSchema != null) {
            if (inputSchema.containsKey("properties")) {
                parametersBuilder.putAdditionalProperty("properties",
                        JsonValue.from(inputSchema.get("properties")));
            }
            if (inputSchema.containsKey("required")) {
                parametersBuilder.putAdditionalProperty("required",
                        JsonValue.from(inputSchema.get("required")));
            }
        } else {
            parametersBuilder.putAdditionalProperty("properties", JsonValue.from(new HashMap<>()));
        }

        parametersBuilder.putAdditionalProperty("additionalProperties", JsonValue.from(false));

        return parametersBuilder.build();
    }

    /**
     * 获取所有已启用的插件的工具
     * 
     * @return 工具信息列表
     */
    public List<ToolInfo> getAllEnabledTools() {
        List<ToolInfo> toolInfoList = new ArrayList<>();

        LambdaQueryWrapper<Plugin> pluginQuery = new LambdaQueryWrapper<>();
        pluginQuery.eq(Plugin::getIsEnabled, true);
        List<Plugin> plugins = pluginMapper.selectList(pluginQuery);

        for (Plugin plugin : plugins) {
            List<ToolInfo> pluginTools = buildToolsFromPlugin(plugin.getId());
            toolInfoList.addAll(pluginTools);
        }

        return toolInfoList;
    }

    /**
     * 将 ToolInfo 列表转换为 ChatCompletionFunctionTool 列表
     * 
     * @param toolInfoList 工具信息列表
     * @return 工具列表
     */
    public List<ChatCompletionFunctionTool> toFunctionTools(List<ToolInfo> toolInfoList) {
        return toolInfoList.stream()
                .map(ToolInfo::getTool)
                .toList();
    }

    /**
     * 根据函数名查找工具信息
     * 
     * @param functionName 函数名
     * @param toolInfoList 工具信息列表
     * @return 工具信息，如果找不到返回 null
     */
    public ToolInfo findToolByFunctionName(String functionName, List<ToolInfo> toolInfoList) {
        for (ToolInfo toolInfo : toolInfoList) {
            if (toolInfo.getFunctionName().equals(functionName)) {
                return toolInfo;
            }
        }
        return null;
    }
}
