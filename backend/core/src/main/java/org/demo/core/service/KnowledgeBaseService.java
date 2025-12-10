package org.demo.core.service;

import org.demo.core.model.dto.KnowledgeBaseCreateDTO;
import org.demo.core.model.dto.KnowledgeBasePatchDTO;
import org.demo.core.model.vo.KnowledgeBaseVO;
import org.demo.core.model.vo.PageResult;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     * 
     * @param dto    创建请求
     * @param userId 当前用户ID
     * @return 创建的知识库详情
     */
    KnowledgeBaseVO createKnowledgeBase(KnowledgeBaseCreateDTO dto, String userId);

    /**
     * 分页获取知识库列表（支持搜索、筛选）
     * 
     * @param page        页码（从1开始）
     * @param pageSize    每页大小
     * @param search      搜索关键词（可选）
     * @param scopeType   作用域类型（可选）
     * @param accessLevel 访问级别（可选）
     * @param userId      当前用户ID
     * @return 分页结果
     */
    PageResult<KnowledgeBaseVO> listKnowledgeBases(Integer page, Integer pageSize, 
                                                     String search, String scopeType, 
                                                     String accessLevel, String userId);

    /**
     * 获取知识库详情
     * 
     * @param uuid   知识库UUID
     * @param userId 当前用户ID
     * @return 知识库详情
     */
    KnowledgeBaseVO getKnowledgeBase(String uuid, String userId);

    /**
     * 更新知识库（部分更新）
     * 
     * @param uuid   知识库UUID
     * @param dto    更新请求
     * @param userId 当前用户ID
     * @return 更新后的知识库详情
     */
    KnowledgeBaseVO updateKnowledgeBase(String uuid, KnowledgeBasePatchDTO dto, String userId);

    /**
     * 删除知识库（级联删除文档和向量数据）
     * 
     * @param uuid   知识库UUID
     * @param userId 当前用户ID
     */
    void deleteKnowledgeBase(String uuid, String userId);

    /**
     * 检查用户是否可以访问知识库
     * 
     * @param userId 用户ID
     * @param kbId   知识库ID
     * @return 是否可以访问
     */
    boolean canAccessKb(String userId, String kbId);
}
