package org.demo.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.mapper.AgentKnowledgeBaseMapper;
import org.demo.core.mapper.AgentMapper;
import org.demo.core.mapper.KnowledgeBaseMapper;
import org.demo.core.model.entity.Agent;
import org.demo.core.model.entity.AgentKnowledgeBase;
import org.demo.core.model.entity.KnowledgeBase;
import org.demo.core.model.vo.KnowledgeBaseVO;
import org.demo.core.service.AgentKnowledgeBaseService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能体知识库关联服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentKnowledgeBaseServiceImpl implements AgentKnowledgeBaseService {

    private final AgentKnowledgeBaseMapper agentKnowledgeBaseMapper;
    private final AgentMapper agentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentKnowledgeBase addKnowledgeBaseToAgent(String agentId, String knowledgeBaseId, Integer priority) {
        log.info("添加知识库关联，agentId: {}, knowledgeBaseId: {}, priority: {}", agentId, knowledgeBaseId, priority);

        // 验证智能体是否存在
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在: " + agentId);
        }

        // 验证知识库是否存在
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在: " + knowledgeBaseId);
        }

        // 检查是否已经存在关联
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
                .eq(AgentKnowledgeBase::getKnowledgeBaseId, knowledgeBaseId);
        
        AgentKnowledgeBase existing = agentKnowledgeBaseMapper.selectOne(wrapper);
        if (existing != null) {
            throw new IllegalArgumentException("该智能体已关联此知识库");
        }

        // 创建关联记录
        AgentKnowledgeBase association = new AgentKnowledgeBase();
        association.setAgentId(agentId);
        association.setKnowledgeBaseId(knowledgeBaseId);
        association.setPriority(priority != null ? priority : 0);
        association.setIsEnabled(true);

        agentKnowledgeBaseMapper.insert(association);
        log.info("知识库关联创建成功，id: {}", association.getId());

        return association;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AgentKnowledgeBase> batchAddKnowledgeBasesToAgent(String agentId, List<String> knowledgeBaseIds) {
        log.info("批量添加知识库关联，agentId: {}, knowledgeBaseIds: {}", agentId, knowledgeBaseIds);

        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<AgentKnowledgeBase> results = new ArrayList<>();
        for (int i = 0; i < knowledgeBaseIds.size(); i++) {
            String kbId = knowledgeBaseIds.get(i);
            try {
                // 使用索引作为默认优先级，后添加的优先级更低
                AgentKnowledgeBase association = addKnowledgeBaseToAgent(agentId, kbId, knowledgeBaseIds.size() - i);
                results.add(association);
            } catch (IllegalArgumentException e) {
                log.warn("跳过知识库关联: {}", e.getMessage());
            }
        }

        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeKnowledgeBaseFromAgent(String agentId, String knowledgeBaseId) {
        log.info("移除知识库关联，agentId: {}, knowledgeBaseId: {}", agentId, knowledgeBaseId);

        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
                .eq(AgentKnowledgeBase::getKnowledgeBaseId, knowledgeBaseId);

        int deleted = agentKnowledgeBaseMapper.delete(wrapper);
        if (deleted == 0) {
            throw new IllegalArgumentException("未找到该关联记录");
        }

        log.info("知识库关联已删除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveKnowledgeBasesFromAgent(String agentId, List<String> knowledgeBaseIds) {
        log.info("批量移除知识库关联，agentId: {}, knowledgeBaseIds: {}", agentId, knowledgeBaseIds);

        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
                .in(AgentKnowledgeBase::getKnowledgeBaseId, knowledgeBaseIds);

        int deleted = agentKnowledgeBaseMapper.delete(wrapper);
        log.info("批量删除知识库关联，删除数量: {}", deleted);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentKnowledgeBase updatePriority(String agentId, String knowledgeBaseId, Integer priority) {
        log.info("更新知识库关联优先级，agentId: {}, knowledgeBaseId: {}, priority: {}", agentId, knowledgeBaseId, priority);

        AgentKnowledgeBase association = getAssociation(agentId, knowledgeBaseId);
        if (association == null) {
            throw new IllegalArgumentException("未找到该关联记录");
        }

        association.setPriority(priority);
        agentKnowledgeBaseMapper.updateById(association);

        log.info("知识库关联优先级已更新");
        return association;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentKnowledgeBase toggleEnabled(String agentId, String knowledgeBaseId, Boolean enabled) {
        log.info("切换知识库关联状态，agentId: {}, knowledgeBaseId: {}, enabled: {}", agentId, knowledgeBaseId, enabled);

        AgentKnowledgeBase association = getAssociation(agentId, knowledgeBaseId);
        if (association == null) {
            throw new IllegalArgumentException("未找到该关联记录");
        }

        association.setIsEnabled(enabled);
        agentKnowledgeBaseMapper.updateById(association);

        log.info("知识库关联状态已更新");
        return association;
    }

    @Override
    public List<KnowledgeBaseVO> getKnowledgeBasesByAgentId(String agentId) {
        log.info("获取智能体关联的知识库，agentId: {}", agentId);

        // 查询关联记录
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
                .orderByDesc(AgentKnowledgeBase::getPriority)
                .orderByDesc(AgentKnowledgeBase::getCreateTime);

        List<AgentKnowledgeBase> associations = agentKnowledgeBaseMapper.selectList(wrapper);

        // 获取知识库详情
        List<String> kbIds = associations.stream()
                .map(AgentKnowledgeBase::getKnowledgeBaseId)
                .collect(Collectors.toList());

        if (kbIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectBatchIds(kbIds);

        // 转换为VO
        return knowledgeBases.stream().map(kb -> {
            KnowledgeBaseVO vo = new KnowledgeBaseVO();
            BeanUtils.copyProperties(kb, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeBaseVO> getEnabledKnowledgeBasesByAgentId(String agentId) {
        log.info("获取智能体启用的知识库，agentId: {}", agentId);

        // 查询启用的关联记录
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
                .eq(AgentKnowledgeBase::getIsEnabled, true)
                .orderByDesc(AgentKnowledgeBase::getPriority)
                .orderByDesc(AgentKnowledgeBase::getCreateTime);

        List<AgentKnowledgeBase> associations = agentKnowledgeBaseMapper.selectList(wrapper);

        // 获取知识库详情
        List<String> kbIds = associations.stream()
                .map(AgentKnowledgeBase::getKnowledgeBaseId)
                .collect(Collectors.toList());

        if (kbIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectBatchIds(kbIds);

        // 转换为VO
        return knowledgeBases.stream().map(kb -> {
            KnowledgeBaseVO vo = new KnowledgeBaseVO();
            BeanUtils.copyProperties(kb, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getAgentIdsByKnowledgeBaseId(String knowledgeBaseId) {
        log.info("获取使用知识库的智能体列表，knowledgeBaseId: {}", knowledgeBaseId);

        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getKnowledgeBaseId, knowledgeBaseId);

        List<AgentKnowledgeBase> associations = agentKnowledgeBaseMapper.selectList(wrapper);

        return associations.stream()
                .map(AgentKnowledgeBase::getAgentId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean isKnowledgeBaseLinkedToAgent(String agentId, String knowledgeBaseId) {
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
                .eq(AgentKnowledgeBase::getKnowledgeBaseId, knowledgeBaseId);

        return agentKnowledgeBaseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public AgentKnowledgeBase getAssociation(String agentId, String knowledgeBaseId) {
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
                .eq(AgentKnowledgeBase::getKnowledgeBaseId, knowledgeBaseId);

        return agentKnowledgeBaseMapper.selectOne(wrapper);
    }
}
