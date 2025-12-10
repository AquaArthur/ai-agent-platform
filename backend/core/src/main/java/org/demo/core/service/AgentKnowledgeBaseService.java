package org.demo.core.service;

import org.demo.core.model.entity.AgentKnowledgeBase;
import org.demo.core.model.vo.KnowledgeBaseVO;

import java.util.List;

/**
 * 智能体知识库关联服务接口
 */
public interface AgentKnowledgeBaseService {

    /**
     * 为智能体添加知识库关联
     * 
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @param priority        优先级（可选，默认0）
     * @return 创建的关联记录
     */
    AgentKnowledgeBase addKnowledgeBaseToAgent(String agentId, String knowledgeBaseId, Integer priority);

    /**
     * 批量为智能体添加知识库关联
     * 
     * @param agentId          智能体ID
     * @param knowledgeBaseIds 知识库ID列表
     * @return 创建的关联记录列表
     */
    List<AgentKnowledgeBase> batchAddKnowledgeBasesToAgent(String agentId, List<String> knowledgeBaseIds);

    /**
     * 移除智能体与知识库的关联
     * 
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     */
    void removeKnowledgeBaseFromAgent(String agentId, String knowledgeBaseId);

    /**
     * 批量移除智能体与知识库的关联
     * 
     * @param agentId          智能体ID
     * @param knowledgeBaseIds 知识库ID列表
     */
    void batchRemoveKnowledgeBasesFromAgent(String agentId, List<String> knowledgeBaseIds);

    /**
     * 更新知识库关联的优先级
     * 
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @param priority        新优先级
     * @return 更新后的关联记录
     */
    AgentKnowledgeBase updatePriority(String agentId, String knowledgeBaseId, Integer priority);

    /**
     * 启用或禁用知识库关联
     * 
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @param enabled         是否启用
     * @return 更新后的关联记录
     */
    AgentKnowledgeBase toggleEnabled(String agentId, String knowledgeBaseId, Boolean enabled);

    /**
     * 获取智能体关联的所有知识库
     * 
     * @param agentId 智能体ID
     * @return 知识库列表（按优先级降序）
     */
    List<KnowledgeBaseVO> getKnowledgeBasesByAgentId(String agentId);

    /**
     * 获取智能体关联的启用状态的知识库
     * 
     * @param agentId 智能体ID
     * @return 启用的知识库列表（按优先级降序）
     */
    List<KnowledgeBaseVO> getEnabledKnowledgeBasesByAgentId(String agentId);

    /**
     * 获取使用某个知识库的所有智能体ID列表
     * 
     * @param knowledgeBaseId 知识库ID
     * @return 智能体ID列表
     */
    List<String> getAgentIdsByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 检查智能体是否关联了指定知识库
     * 
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @return 是否存在关联
     */
    boolean isKnowledgeBaseLinkedToAgent(String agentId, String knowledgeBaseId);

    /**
     * 获取智能体与知识库的关联记录
     * 
     * @param agentId         智能体ID
     * @param knowledgeBaseId 知识库ID
     * @return 关联记录，不存在则返回null
     */
    AgentKnowledgeBase getAssociation(String agentId, String knowledgeBaseId);
}
