package org.demo.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.demo.core.api.ApiResponse;
import org.demo.core.model.entity.AgentKnowledgeBase;
import org.demo.core.model.vo.KnowledgeBaseVO;
import org.demo.core.service.AgentKnowledgeBaseService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 智能体知识库关联控制器
 * 提供智能体与知识库关联关系的管理接口
 */
@Tag(name = "智能体知识库关联", description = "管理智能体与知识库之间的关联关系")
@RestController
@RequestMapping("/api/v1/agents/{agentId}/knowledge-bases")
@RequiredArgsConstructor
@Validated
public class AgentKnowledgeBaseController {

    private final AgentKnowledgeBaseService agentKnowledgeBaseService;

    /**
     * 为智能体添加知识库关联
     * POST /api/v1/agents/{agentId}/knowledge-bases
     *
     * @param agentId 智能体ID
     * @param request 请求体
     * @return 创建的关联记录
     */
    @Operation(summary = "为智能体添加知识库", description = "为指定智能体添加一个知识库关联")
    @PostMapping
    public ApiResponse<AgentKnowledgeBase> addKnowledgeBase(
            @Parameter(description = "智能体ID", required = true)
            @PathVariable String agentId,
            @Valid @RequestBody AddKnowledgeBaseRequest request) {

        AgentKnowledgeBase result = agentKnowledgeBaseService.addKnowledgeBaseToAgent(
                agentId, request.getKnowledgeBaseId(), request.getPriority());

        return ApiResponse.ok("知识库添加成功", result);
    }

    /**
     * 批量为智能体添加知识库关联
     * POST /api/v1/agents/{agentId}/knowledge-bases/batch
     *
     * @param agentId 智能体ID
     * @param request 请求体
     * @return 创建的关联记录列表
     */
    @Operation(summary = "批量添加知识库", description = "为指定智能体批量添加多个知识库关联")
    @PostMapping("/batch")
    public ApiResponse<List<AgentKnowledgeBase>> batchAddKnowledgeBases(
            @Parameter(description = "智能体ID", required = true)
            @PathVariable String agentId,
            @Valid @RequestBody BatchAddKnowledgeBasesRequest request) {

        List<AgentKnowledgeBase> results = agentKnowledgeBaseService.batchAddKnowledgeBasesToAgent(
                agentId, request.getKnowledgeBaseIds());

        return ApiResponse.ok("批量添加成功", results);
    }

    /**
     * 获取智能体关联的所有知识库
     * GET /api/v1/agents/{agentId}/knowledge-bases
     *
     * @param agentId     智能体ID
     * @param enabledOnly 是否只获取启用的知识库
     * @return 知识库列表
     */
    @Operation(summary = "获取智能体关联的知识库", description = "获取指定智能体关联的所有知识库列表")
    @GetMapping
    public ApiResponse<List<KnowledgeBaseVO>> getKnowledgeBases(
            @Parameter(description = "智能体ID", required = true)
            @PathVariable String agentId,
            @Parameter(description = "是否只返回启用的知识库", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean enabledOnly) {

        List<KnowledgeBaseVO> results = enabledOnly
                ? agentKnowledgeBaseService.getEnabledKnowledgeBasesByAgentId(agentId)
                : agentKnowledgeBaseService.getKnowledgeBasesByAgentId(agentId);

        return ApiResponse.ok("查询成功", results);
    }

    /**
     * 移除智能体的知识库关联
     * DELETE /api/v1/agents/{agentId}/knowledge-bases/{knowledgeBaseId}
     *
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @return 操作结果
     */
    @Operation(summary = "移除知识库关联", description = "移除智能体与指定知识库的关联")
    @DeleteMapping("/{knowledgeBaseId}")
    public ApiResponse<Void> removeKnowledgeBase(
            @Parameter(description = "智能体ID", required = true)
            @PathVariable String agentId,
            @Parameter(description = "知识库ID", required = true)
            @PathVariable String knowledgeBaseId) {

        agentKnowledgeBaseService.removeKnowledgeBaseFromAgent(agentId, knowledgeBaseId);

        return ApiResponse.ok();
    }

    /**
     * 批量移除智能体的知识库关联
     * DELETE /api/v1/agents/{agentId}/knowledge-bases/batch
     *
     * @param agentId 智能体ID
     * @param request 请求体
     * @return 操作结果
     */
    @Operation(summary = "批量移除知识库关联", description = "批量移除智能体与多个知识库的关联")
    @DeleteMapping("/batch")
    public ApiResponse<Void> batchRemoveKnowledgeBases(
            @Parameter(description = "智能体ID", required = true)
            @PathVariable String agentId,
            @Valid @RequestBody BatchRemoveKnowledgeBasesRequest request) {

        agentKnowledgeBaseService.batchRemoveKnowledgeBasesFromAgent(agentId, request.getKnowledgeBaseIds());

        return ApiResponse.ok();
    }

    /**
     * 更新知识库关联的优先级
     * PATCH /api/v1/agents/{agentId}/knowledge-bases/{knowledgeBaseId}/priority
     *
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @param request         请求体
     * @return 更新后的关联记录
     */
    @Operation(summary = "更新知识库优先级", description = "更新智能体关联的知识库的优先级")
    @PatchMapping("/{knowledgeBaseId}/priority")
    public ApiResponse<AgentKnowledgeBase> updatePriority(
            @Parameter(description = "智能体ID", required = true)
            @PathVariable String agentId,
            @Parameter(description = "知识库ID", required = true)
            @PathVariable String knowledgeBaseId,
            @Valid @RequestBody UpdatePriorityRequest request) {

        AgentKnowledgeBase result = agentKnowledgeBaseService.updatePriority(
                agentId, knowledgeBaseId, request.getPriority());

        return ApiResponse.ok("优先级更新成功", result);
    }

    /**
     * 启用或禁用知识库关联
     * PATCH /api/v1/agents/{agentId}/knowledge-bases/{knowledgeBaseId}/toggle
     *
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @param request         请求体
     * @return 更新后的关联记录
     */
    @Operation(summary = "切换知识库启用状态", description = "启用或禁用智能体关联的知识库")
    @PatchMapping("/{knowledgeBaseId}/toggle")
    public ApiResponse<AgentKnowledgeBase> toggleEnabled(
            @Parameter(description = "智能体ID", required = true)
            @PathVariable String agentId,
            @Parameter(description = "知识库ID", required = true)
            @PathVariable String knowledgeBaseId,
            @Valid @RequestBody ToggleEnabledRequest request) {

        AgentKnowledgeBase result = agentKnowledgeBaseService.toggleEnabled(
                agentId, knowledgeBaseId, request.getEnabled());

        return ApiResponse.ok("状态更新成功", result);
    }

    /**
     * 检查智能体是否关联了指定知识库
     * GET /api/v1/agents/{agentId}/knowledge-bases/{knowledgeBaseId}/exists
     *
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @return 是否存在关联
     */
    @Operation(summary = "检查知识库关联", description = "检查智能体是否关联了指定知识库")
    @GetMapping("/{knowledgeBaseId}/exists")
    public ApiResponse<Boolean> checkAssociation(
            @Parameter(description = "智能体ID", required = true)
            @PathVariable String agentId,
            @Parameter(description = "知识库ID", required = true)
            @PathVariable String knowledgeBaseId) {

        boolean exists = agentKnowledgeBaseService.isKnowledgeBaseLinkedToAgent(agentId, knowledgeBaseId);

        return ApiResponse.ok("查询成功", exists);
    }

    // ==================== 请求和响应DTO ====================

    /**
     * 添加知识库请求
     */
    @Data
    public static class AddKnowledgeBaseRequest {
        @NotBlank(message = "知识库ID不能为空")
        private String knowledgeBaseId;

        private Integer priority;
    }

    /**
     * 批量添加知识库请求
     */
    @Data
    public static class BatchAddKnowledgeBasesRequest {
        @NotNull(message = "知识库ID列表不能为空")
        private List<String> knowledgeBaseIds;
    }

    /**
     * 批量移除知识库请求
     */
    @Data
    public static class BatchRemoveKnowledgeBasesRequest {
        @NotNull(message = "知识库ID列表不能为空")
        private List<String> knowledgeBaseIds;
    }

    /**
     * 更新优先级请求
     */
    @Data
    public static class UpdatePriorityRequest {
        @NotNull(message = "优先级不能为空")
        private Integer priority;
    }

    /**
     * 切换启用状态请求
     */
    @Data
    public static class ToggleEnabledRequest {
        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
    }
}
