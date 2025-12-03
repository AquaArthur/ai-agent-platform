package org.demo.core.service;

import org.demo.core.model.vo.DocumentVO;
import org.demo.core.model.vo.PageResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档服务接口
 */
public interface DocumentService {

    /**
     * 分页获取知识库的文档列表
     * 
     * @param kbUuid   知识库UUID
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @param status   文档状态（可选）
     * @param userId   当前用户ID
     * @return 分页结果
     */
    PageResult<DocumentVO> listDocuments(String kbUuid, Integer page, Integer pageSize, String status, String userId);

    /**
     * 上传文档到知识库
     * 
     * @param kbUuid 知识库UUID
     * @param file   上传的文件
     * @param userId 当前用户ID
     * @return 创建的文档详情
     */
    DocumentVO uploadDocument(String kbUuid, MultipartFile file, String userId);

    /**
     * 获取文档详情
     * 
     * @param uuid   文档UUID
     * @param userId 当前用户ID
     * @return 文档详情
     */
    DocumentVO getDocument(String uuid, String userId);

    /**
     * 删除文档（级联删除文档块和向量数据）
     * 
     * @param uuid   文档UUID
     * @param userId 当前用户ID
     */
    void deleteDocument(String uuid, String userId);

    /**
     * 异步处理文档（解析、分块、向量化）
     * 
     * @param documentId 文档ID
     */
    void processDocumentAsync(String documentId);
}
