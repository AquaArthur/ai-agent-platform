package org.demo.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.demo.core.api.ApiResponse;
import org.demo.core.model.vo.DocumentVO;
import org.demo.core.model.vo.PageResult;
import org.demo.core.service.DocumentService;import org.demo.core.util.SecurityUtil;import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档控制器
 * 提供文档的CRUD操作接口
 */
@Tag(name = "文档管理", description = "提供文档的上传、查询、删除等管理功能接口")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 获取知识库的文档列表（支持分页、状态筛选）
     * GET /api/v1/knowledge-bases/{kbUuid}/documents
     *
     * @param kbUuid   知识库UUID
     * @param page     页码（默认1）
     * @param pageSize 每页大小（默认10）
     * @param status   文档状态（可选）
     * @return 分页结果
     */
    @Operation(summary = "获取知识库的文档列表", description = "分页获取指定知识库的文档列表，支持按状态筛选")
    @GetMapping("/knowledge-bases/{kbUuid}/documents")
    public ApiResponse<PageResult<DocumentVO>> listDocuments(
            @Parameter(description = "知识库UUID", required = true)
            @PathVariable String kbUuid,
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "文档状态（uploading/processing/processed/failed）")
            @RequestParam(required = false) String status) {
        // 从Security Context获取当前用户ID
        String currentUserId = SecurityUtil.getCurrentUserId();

        PageResult<DocumentVO> result = documentService.listDocuments(kbUuid, page, pageSize, status, currentUserId);
        return ApiResponse.ok("查询成功", result);
    }

    /**
     * 上传文档到知识库
     * POST /api/v1/knowledge-bases/{kbUuid}/documents
     *
     * @param kbUuid 知识库UUID
     * @param file   上传的文件
     * @return 创建的文档详情
     */
    @Operation(summary = "上传文档", description = "上传文档到指定知识库（仅支持TXT和Markdown格式，最大10MB）")
    @PostMapping("/knowledge-bases/{kbUuid}/documents")
    public ApiResponse<DocumentVO> uploadDocument(
            @Parameter(description = "知识库UUID", required = true)
            @PathVariable String kbUuid,
            @Parameter(description = "上传的文件", required = true)
            @RequestParam("file") MultipartFile file) {
        // 从Security Context获取当前用户ID
        String currentUserId = SecurityUtil.getCurrentUserId();

        DocumentVO result = documentService.uploadDocument(kbUuid, file, currentUserId);
        return ApiResponse.ok("上传成功", result);
    }

    /**
     * 获取文档详情
     * GET /api/v1/documents/{uuid}
     *
     * @param uuid 文档UUID
     * @return 文档详情
     */
    @Operation(summary = "获取文档详情", description = "通过UUID获取文档的详细信息")
    @GetMapping("/documents/{uuid}")
    public ApiResponse<DocumentVO> getDocument(
            @Parameter(description = "文档UUID", required = true)
            @PathVariable String uuid) {
        // 从Security Context获取当前用户ID
        String currentUserId = SecurityUtil.getCurrentUserId();

        DocumentVO result = documentService.getDocument(uuid, currentUserId);
        return ApiResponse.ok("查询成功", result);
    }

    /**
     * 删除文档
     * DELETE /api/v1/documents/{uuid}
     *
     * @param uuid 文档UUID
     * @return 删除结果
     */
    @Operation(summary = "删除文档", description = "删除指定文档（级联删除文档块和向量数据）")
    @DeleteMapping("/documents/{uuid}")
    public ApiResponse<Object> deleteDocument(
            @Parameter(description = "文档UUID", required = true)
            @PathVariable String uuid) {
        // 从Security Context获取当前用户ID
        String currentUserId = SecurityUtil.getCurrentUserId();

        documentService.deleteDocument(uuid, currentUserId);
        return ApiResponse.ok();
    }
}
