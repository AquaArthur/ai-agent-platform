package org.demo.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.demo.core.api.ApiResponse;
import org.demo.core.model.dto.KnowledgeBaseCreateDTO;
import org.demo.core.model.dto.KnowledgeBasePatchDTO;
import org.demo.core.model.vo.KnowledgeBaseVO;
import org.demo.core.model.vo.PageResult;
import org.demo.core.service.KnowledgeBaseService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库控制器
 * 提供知识库的CRUD操作接口
 */
@Tag(name = "知识库管理", description = "提供知识库的增删改查等管理功能接口")
@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     * POST /api/v1/knowledge-bases
     *
     * @param dto 创建请求
     * @return 创建的知识库详情
     */
    @Operation(summary = "创建知识库", description = "用户输入知识库名称、描述，选择知识库级别，配置分块参数，创建新的知识库")
    @PostMapping
    public ApiResponse<KnowledgeBaseVO> createKnowledgeBase(
            @Validated @RequestBody KnowledgeBaseCreateDTO dto) {
        // TODO: 从认证上下文获取当前用户ID
        String currentUserId = "user-002-home"; // 临时硬编码，实际应从Security Context获取
        
        KnowledgeBaseVO result = knowledgeBaseService.createKnowledgeBase(dto, currentUserId);
        return ApiResponse.ok("创建成功", result);
    }

    /**
     * 获取知识库列表（支持分页、搜索、筛选）
     * GET /api/v1/knowledge-bases
     *
     * @param page        页码（默认1）
     * @param pageSize    每页大小（默认10）
     * @param search      搜索关键词（可选）
     * @param scopeType   作用域类型（可选）
     * @param accessLevel 访问级别（可选）
     * @return 分页结果
     */
    @Operation(summary = "获取知识库列表", description = "分页获取知识库列表，支持按名称搜索、按作用域类型和访问级别筛选")
    @GetMapping
    public ApiResponse<PageResult<KnowledgeBaseVO>> listKnowledgeBases(
            @Parameter(description = "页码，从1开始", example = "1") 
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") 
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") 
            @RequestParam(required = false) String search,
            @Parameter(description = "作用域类型（system/school/course/agent/personal）") 
            @RequestParam(required = false) String scopeType,
            @Parameter(description = "访问级别（public/protected/private）") 
            @RequestParam(required = false) String accessLevel) {
        // TODO: 从认证上下文获取当前用户ID
        String currentUserId = "user-002-home"; // 临时硬编码
        
        PageResult<KnowledgeBaseVO> result = knowledgeBaseService.listKnowledgeBases(
                page, pageSize, search, scopeType, accessLevel, currentUserId);
        return ApiResponse.ok("查询成功", result);
    }

    /**
     * 获取知识库详情
     * GET /api/v1/knowledge-bases/{uuid}
     *
     * @param uuid 知识库UUID
     * @return 知识库详情
     */
    @Operation(summary = "获取知识库详情", description = "通过UUID获取知识库的详细信息")
    @GetMapping("/{uuid}")
    public ApiResponse<KnowledgeBaseVO> getKnowledgeBase(
            @Parameter(description = "知识库UUID", required = true) 
            @PathVariable String uuid) {
        // TODO: 从认证上下文获取当前用户ID
        String currentUserId = "user-002-home"; // 临时硬编码
        
        KnowledgeBaseVO result = knowledgeBaseService.getKnowledgeBase(uuid, currentUserId);
        return ApiResponse.ok("查询成功", result);
    }

    /**
     * 更新知识库（部分更新）
     * PATCH /api/v1/knowledge-bases/{uuid}
     *
     * @param uuid 知识库UUID
     * @param dto  更新请求
     * @return 更新后的知识库详情
     */
    @Operation(summary = "更新知识库", description = "修改知识库的基本信息、分块参数、访问权限等配置，支持部分字段更新")
    @PatchMapping("/{uuid}")
    public ApiResponse<KnowledgeBaseVO> updateKnowledgeBase(
            @Parameter(description = "知识库UUID", required = true) 
            @PathVariable String uuid,
            @Validated @RequestBody KnowledgeBasePatchDTO dto) {
        // TODO: 从认证上下文获取当前用户ID
        String currentUserId = "user-002-home"; // 临时硬编码
        
        KnowledgeBaseVO result = knowledgeBaseService.updateKnowledgeBase(uuid, dto, currentUserId);
        return ApiResponse.ok("更新成功", result);
    }

    /**
     * 删除知识库
     * DELETE /api/v1/knowledge-bases/{uuid}
     *
     * @param uuid 知识库UUID
     * @return 删除结果
     */
    @Operation(summary = "删除知识库", description = "删除知识库，级联删除关联的文档和向量数据")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteKnowledgeBase(
            @Parameter(description = "知识库UUID", required = true) 
            @PathVariable String uuid) {
        // TODO: 从认证上下文获取当前用户ID
        String currentUserId = "user-002-home"; // 临时硬编码
        
        knowledgeBaseService.deleteKnowledgeBase(uuid, currentUserId);
        return ApiResponse.ok("删除成功", null);
    }
}
