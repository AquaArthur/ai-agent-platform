package org.demo.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.api.ApiResponse;
import org.demo.core.mapper.*;
import org.demo.core.model.dto.RAGQueryData;
import org.demo.core.model.dto.RAGQueryResultItem;
import org.demo.core.model.entity.*;
import org.demo.core.model.vo.PluginInvokeResult;
import org.demo.core.util.FunctionNameMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LLM 服务类
 * 负责与大语言模型进行交互
 * 
 * 特性：
 * - 自动检测智能体是否绑定插件
 * - 有插件时自动启用 Function Calling
 * - 支持多轮工具调用
 * - 支持 RAG 知识库检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmModelMapper llmModelMapper;
    private final LlmProviderMapper llmProviderMapper;
    private final AgentMapper agentMapper;
    private final PluginMapper pluginMapper;
    private final PluginService pluginService;
    private final ToolDefinitionService toolDefinitionService;
    private final ObjectMapper objectMapper;
    private final AgentKnowledgeBaseMapper agentKnowledgeBaseMapper;

    // 最大工具调用轮次，防止无限循环
    private static final int MAX_TOOL_CALL_ROUNDS = 5;

    /**
     * 智能聊天方法 - 自动判断是否启用 Function Calling
     * 
     * @param agentId             智能体ID
     * @param llmModelId          LLM模型ID
     * @param userQuery           用户问题
     * @param conversationHistory 会话历史（可选）
     * @return 大模型的回答
     */
    public String chat(String agentId, String llmModelId, String userQuery,
            List<Map<String, String>> conversationHistory) {
        try {
            // 1. 查询 Agent
            Agent agent = agentMapper.selectById(agentId);
            if (agent == null) {
                throw new RuntimeException("Agent不存在: " + agentId);
            }

            // 2. 查询 LlmModel 信息
            LlmModel llmModel = llmModelMapper.selectById(llmModelId);
            if (llmModel == null) {
                throw new RuntimeException("LLM模型不存在: " + llmModelId);
            }

            // 3. 查询 Provider 信息
            QueryWrapper<LlmProvider> providerQuery = new QueryWrapper<>();
            providerQuery.eq("code", llmModel.getProvider());
            LlmProvider provider = llmProviderMapper.selectOne(providerQuery);
            if (provider == null) {
                throw new RuntimeException("Provider不存在: " + llmModel.getProvider());
            }

            // 4. 构建 OpenAI 客户端
            OpenAIClient client = buildClient(llmModel, provider);

            // 5. 使用 ToolDefinitionService 构建工具列表
            List<ToolDefinitionService.ToolInfo> toolInfoList = toolDefinitionService.buildToolsFromAgent(agent);
            List<ChatCompletionFunctionTool> tools = toolDefinitionService.toFunctionTools(toolInfoList);
            boolean hasFunctionCalling = !tools.isEmpty();

            log.info("智能体配置: agentId={}, 绑定插件数={}, Function Calling={}",
                    agentId, tools.size(), hasFunctionCalling ? "启用" : "禁用");

            // 6. 构建请求参数
            ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder()
                    .model(llmModel.getName());

            // 添加系统提示词
            if (agent.getPrompt() != null && !agent.getPrompt().isEmpty()) {
                paramsBuilder.addSystemMessage(agent.getPrompt());
            }

            // 添加 RAG 知识库内容
            String ragContext = buildRagContext(agentId, userQuery);
            if (ragContext != null && !ragContext.isEmpty()) {
                paramsBuilder.addSystemMessage(ragContext);
            }

            // 添加历史对话
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                for (Map<String, String> msg : conversationHistory) {
                    String role = msg.get("role");
                    String content = msg.get("content");
                    if ("user".equals(role)) {
                        paramsBuilder.addUserMessage(content);
                    } else if ("assistant".equals(role)) {
                        paramsBuilder.addAssistantMessage(content);
                    }
                }
            }

            // 添加当前用户问题
            paramsBuilder.addUserMessage(userQuery);

            // 设置温度等参数
            if (llmModel.getTemperature() != null) {
                paramsBuilder.temperature(llmModel.getTemperature().doubleValue());
            }
            if (llmModel.getMaxTokens() != null) {
                paramsBuilder.maxCompletionTokens(llmModel.getMaxTokens());
            }

            // 7. 根据是否有工具决定调用方式
            if (hasFunctionCalling) {
                // 添加工具
                for (ChatCompletionFunctionTool tool : tools) {
                    paramsBuilder.addTool(tool);
                }
                // 执行带工具调用的对话
                return executeWithToolCalls(client, paramsBuilder, llmModel.getName());
            } else {
                // 简单对话（无工具）
                return executeSimpleChat(client, paramsBuilder, llmModel.getName());
            }

        } catch (Exception e) {
            log.error("LLM调用失败", e);
            throw new RuntimeException("调用大模型失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简化版聊天方法（不带历史记录）
     */
    public String chat(String agentId, String llmModelId, String userQuery) {
        return chat(agentId, llmModelId, userQuery, null);
    }

    // ==================== 私有方法 ====================

    /**
     * 构建 OpenAI 客户端
     */
    private OpenAIClient buildClient(LlmModel llmModel, LlmProvider provider) {
        String apiKey = llmModel.getApiKey();
        String baseUrl = llmModel.getApiBase() != null ? llmModel.getApiBase() : provider.getDefaultApiBase();

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key未配置");
        }

        log.debug("构建 OpenAI 客户端: baseUrl={}, model={}", baseUrl, llmModel.getName());

        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 执行简单对话（无工具调用）
     */
    private String executeSimpleChat(OpenAIClient client,
            ChatCompletionCreateParams.Builder paramsBuilder,
            String modelName) {

        ChatCompletion completion = client.chat().completions().create(paramsBuilder.build());

        String answer = completion.choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .collect(Collectors.joining());

        log.info("LLM调用成功 - 模型: {}, 输入Token: {}, 输出Token: {}",
                modelName,
                completion.usage().map(u -> u.promptTokens()).orElse(null),
                completion.usage().map(u -> u.completionTokens()).orElse(null));

        return answer;
    }

    /**
     * 执行带工具调用的对话（支持多轮）
     */
    private String executeWithToolCalls(OpenAIClient client,
            ChatCompletionCreateParams.Builder paramsBuilder,
            String modelName) {

        int round = 0;
        List<PluginCallRecord> pluginCallRecords = new ArrayList<>();

        while (round < MAX_TOOL_CALL_ROUNDS) {
            round++;
            log.info("=== 第 {} 轮对话 ===", round);

            // 调用 LLM
            ChatCompletion completion = client.chat().completions().create(paramsBuilder.build());

            log.info("LLM响应 - 模型: {}, 输入Token: {}, 输出Token: {}",
                    modelName,
                    completion.usage().map(u -> u.promptTokens()).orElse(null),
                    completion.usage().map(u -> u.completionTokens()).orElse(null));

            // 获取消息
            ChatCompletionMessage message = completion.choices().get(0).message();

            // 将 assistant 消息添加到对话历史
            paramsBuilder.addMessage(message);

            // 检查是否有工具调用
            List<ChatCompletionMessageToolCall> toolCalls = message.toolCalls().orElse(Collections.emptyList());

            if (toolCalls.isEmpty()) {
                // 没有工具调用，返回最终回答
                String answer = message.content().orElse("抱歉，我无法回答这个问题。");

                // 记录插件调用信息
                if (!pluginCallRecords.isEmpty()) {
                    log.info("本次对话共调用了 {} 次插件", pluginCallRecords.size());
                    for (PluginCallRecord record : pluginCallRecords) {
                        log.info("  - {}: {} -> {}", record.pluginName, record.operationName,
                                record.success ? "成功" : "失败");
                    }
                }

                return answer;
            }

            // 处理工具调用
            log.info("LLM 请求调用 {} 个工具", toolCalls.size());

            for (ChatCompletionMessageToolCall toolCall : toolCalls) {
                // 使用 asFunction() 获取函数调用信息
                ChatCompletionMessageFunctionToolCall functionToolCall = toolCall.asFunction();
                String toolCallId = functionToolCall.id();
                ChatCompletionMessageFunctionToolCall.Function function = functionToolCall.function();
                String functionName = function.name();
                String arguments = function.arguments();

                log.info("执行工具调用: {} (id: {})", functionName, toolCallId);
                log.info("参数: {}", arguments);

                // 执行工具调用
                ToolCallResult result = executeToolCall(functionName, arguments);

                // 记录调用信息
                pluginCallRecords.add(new PluginCallRecord(
                        result.pluginName,
                        result.operationName,
                        result.success,
                        result.resultData != null ? result.resultData.toString() : result.errorMessage));

                // 将工具结果添加到对话历史（使用 contentAsJson 返回结构化数据）
                if (result.resultData != null) {
                    paramsBuilder.addMessage(ChatCompletionToolMessageParam.builder()
                            .toolCallId(toolCallId)
                            .contentAsJson(result.resultData)
                            .build());
                } else {
                    // 失败情况下返回错误字符串
                    paramsBuilder.addMessage(ChatCompletionToolMessageParam.builder()
                            .toolCallId(toolCallId)
                            .content(result.errorMessage != null ? result.errorMessage : "工具调用失败")
                            .build());
                }

                log.info("工具调用结果: {}", result.resultData != null ? result.resultData : result.errorMessage);
            }
        }

        log.warn("达到最大工具调用轮次限制 ({})", MAX_TOOL_CALL_ROUNDS);
        return "抱歉，处理您的请求时遇到了问题，请稍后重试。";
    }

    /**
     * 执行工具调用
     */
    private ToolCallResult executeToolCall(String functionName, String arguments) {
        ToolCallResult result = new ToolCallResult();

        try {
            // 使用 FunctionNameMapper 解析函数名
            FunctionNameMapper.FunctionNameParts parts = FunctionNameMapper.decode(functionName);
            if (!parts.isValid()) {
                result.success = false;
                result.errorMessage = "错误：无效的函数名格式 - " + functionName;
                return result;
            }

            String pluginId = parts.getPluginId();
            String operationId = parts.getOperationId();
            result.operationName = operationId;

            // 获取插件名称
            Plugin plugin = pluginMapper.selectById(pluginId);
            result.pluginName = plugin != null ? plugin.getName() : pluginId;

            // 解析参数
            Map<String, Object> params = new HashMap<>();
            if (arguments != null && !arguments.isEmpty() && !arguments.equals("{}")) {
                params = objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {
                });
            }

            log.info("执行插件调用: pluginId={}, operationId={}, params={}", pluginId, operationId, params);

            // 调用插件
            PluginInvokeResult invokeResult = pluginService.invokeOperation(pluginId, operationId, params, 30000);

            if (invokeResult == null) {
                result.success = false;
                result.errorMessage = "插件调用返回空结果";
                return result;
            }

            if (invokeResult.isSuccess()) {
                result.success = true;
                // 直接存储结构化数据对象，而非序列化后的字符串
                Object parsedData = invokeResult.getParsedData();
                if (parsedData != null) {
                    result.resultData = parsedData;
                } else {
                    // 如果没有解析后的数据，尝试解析原始响应体
                    String rawBody = invokeResult.getRawBody();
                    if (rawBody != null && !rawBody.isEmpty()) {
                        try {
                            result.resultData = objectMapper.readValue(rawBody, Object.class);
                        } catch (Exception e) {
                            // 如果无法解析为JSON，将原始字符串包装为Map
                            result.resultData = Map.of("result", rawBody);
                        }
                    } else {
                        result.resultData = Map.of("result", "操作成功");
                    }
                }
            } else if (invokeResult.isTimeout()) {
                result.success = false;
                result.errorMessage = "插件调用超时";
            } else {
                result.success = false;
                result.errorMessage = "插件调用失败: " + invokeResult.getErrorMessage();
            }

        } catch (JsonProcessingException e) {
            log.error("解析工具调用参数失败", e);
            result.success = false;
            result.errorMessage = "参数解析失败: " + e.getMessage();
        } catch (Exception e) {
            log.error("执行工具调用失败", e);
            result.success = false;
            result.errorMessage = "工具调用失败: " + e.getMessage();
        }

        return result;
    }

    /**
     * 构建 RAG 知识库上下文
     */
    private String buildRagContext(String agentId, String userQuery) {
        try {
            // 查询智能体关联的知识库
            List<AgentKnowledgeBase> akbList = agentKnowledgeBaseMapper.selectList(
                    new LambdaQueryWrapper<AgentKnowledgeBase>()
                            .eq(AgentKnowledgeBase::getAgentId, agentId)
                            .eq(AgentKnowledgeBase::getIsEnabled, 1));

            if (akbList.isEmpty()) {
                return null;
            }

            log.info("智能体绑定了 {} 个知识库用于RAG检索", akbList.size());

            StringBuilder ragMessage = new StringBuilder();
            ragMessage.append("以下是来自知识库的参考信息：\n\n");

            RagClient ragClient = new RagClient(new RestTemplate());
            boolean hasContent = false;

            for (AgentKnowledgeBase akb : akbList) {
                try {
                    ApiResponse<RAGQueryData> response = ragClient.query(
                            akb.getKnowledgeBaseId(), userQuery, 3, 0.5);

                    if (response != null && response.getCode() == 200 && response.getData() != null) {
                        int resultNum = response.getData().getResult_num();
                        if (resultNum > 0) {
                            List<RAGQueryResultItem> results = response.getData().getResults();
                            for (RAGQueryResultItem item : results) {
                                ragMessage.append("- ").append(item.getContent()).append("\n");
                                hasContent = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("RAG检索失败: kbId={}, error={}", akb.getKnowledgeBaseId(), e.getMessage());
                }
            }

            if (hasContent) {
                ragMessage.append("\n请根据以上信息回答用户问题。如果信息中没有相关内容，可以直接说明。");
                return ragMessage.toString();
            }

            return null;

        } catch (Exception e) {
            log.error("构建RAG上下文失败", e);
            return null;
        }
    }

    // ==================== 内部类 ====================

    /**
     * 工具调用结果
     * 使用结构化数据对象而非字符串，以便使用 contentAsJson() 返回
     */
    private static class ToolCallResult {
        String pluginName = "未知插件";
        String operationName = "未知操作";
        boolean success = false;
        Object resultData = null; // 成功时的结构化数据
        String errorMessage = null; // 失败时的错误消息
    }

    /**
     * 插件调用记录
     */
    private static class PluginCallRecord {
        String pluginName;
        String operationName;
        boolean success;
        String result;

        PluginCallRecord(String pluginName, String operationName, boolean success, String result) {
            this.pluginName = pluginName;
            this.operationName = operationName;
            this.success = success;
            this.result = result;
        }
    }
}
