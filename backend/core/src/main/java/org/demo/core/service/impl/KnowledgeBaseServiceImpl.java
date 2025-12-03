package org.demo.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.demo.core.mapper.KnowledgeBaseMapper;
import org.demo.core.model.dto.KnowledgeBaseCreateDTO;
import org.demo.core.model.dto.KnowledgeBasePatchDTO;
import org.demo.core.model.entity.KnowledgeBase;
import org.demo.core.model.vo.KnowledgeBaseVO;
import org.demo.core.model.vo.PageResult;
import org.demo.core.service.KnowledgeBaseService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO createKnowledgeBase(KnowledgeBaseCreateDTO dto, String userId) {
        log.info("创建知识库，name: {}, userId: {}", dto.getName(), userId);

        // 验证配置有效性
        if (dto.getChunkSize() != null && (dto.getChunkSize() < 100 || dto.getChunkSize() > 2000)) {
            throw new IllegalArgumentException("chunk_size必须在100-2000之间");
        }

        // 检查名称是否重复
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUserId, userId)
               .eq(KnowledgeBase::getName, dto.getName());
        if (knowledgeBaseMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("知识库名称已存在");
        }

        // 创建知识库实体
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUuid(UUID.randomUUID().toString());
        kb.setName(dto.getName());
        kb.setDescription(dto.getDescription());
        kb.setIcon(dto.getIcon());
        kb.setScopeType(dto.getScopeType());
        kb.setScopeId(dto.getScopeId());
        kb.setParentKbId(dto.getParentKbId());
        kb.setOwnerId(userId);
        kb.setUserId(userId);
        kb.setAccessLevel(StringUtils.defaultIfBlank(dto.getAccessLevel(), "private"));
        kb.setDocumentCount(0);
        kb.setChunkCount(0);
        kb.setTotalSize(0L);
        kb.setChunkSize(dto.getChunkSize() != null ? dto.getChunkSize() : 800);
        kb.setChunkOverlap(dto.getChunkOverlap() != null ? dto.getChunkOverlap() : 50);
        kb.setEmbeddingModelId(dto.getEmbeddingModelId());
        
        // 设置默认检索配置
        if (dto.getRetrievalConfig() != null) {
            kb.setRetrievalConfig(dto.getRetrievalConfig());
        } else {
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("top_k", 5);
            defaultConfig.put("similarity_threshold", 0.7);
            defaultConfig.put("max_context_length", 2000);
            kb.setRetrievalConfig(defaultConfig);
        }

        knowledgeBaseMapper.insert(kb);

        log.info("知识库创建成功，id: {}, uuid: {}", kb.getId(), kb.getUuid());
        return convertToVO(kb);
    }

    @Override
    public PageResult<KnowledgeBaseVO> listKnowledgeBases(Integer page, Integer pageSize, 
                                                           String search, String scopeType, 
                                                           String accessLevel, String userId) {
        log.info("分页查询知识库，page: {}, pageSize: {}, search: {}, scopeType: {}, accessLevel: {}, userId: {}", 
                 page, pageSize, search, scopeType, accessLevel, userId);

        // 构建查询条件
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();

        // 权限过滤：只能看到自己的和公开的
        wrapper.and(w -> w.eq(KnowledgeBase::getUserId, userId)
                          .or()
                          .eq(KnowledgeBase::getAccessLevel, "public"));

        // 搜索条件
        if (StringUtils.isNotBlank(search)) {
            wrapper.and(w -> w.like(KnowledgeBase::getName, search)
                              .or()
                              .like(KnowledgeBase::getDescription, search));
        }

        // 筛选条件
        if (StringUtils.isNotBlank(scopeType)) {
            wrapper.eq(KnowledgeBase::getScopeType, scopeType);
        }
        if (StringUtils.isNotBlank(accessLevel)) {
            wrapper.eq(KnowledgeBase::getAccessLevel, accessLevel);
        }

        // 排序
        wrapper.orderByDesc(KnowledgeBase::getCreateTime);

        // 分页查询
        Page<KnowledgeBase> pageParam = new Page<>(page, pageSize);
        IPage<KnowledgeBase> pageResult = knowledgeBaseMapper.selectPage(pageParam, wrapper);

        // 转换为VO
        List<KnowledgeBaseVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageResult.getTotal(), page, pageSize, voList);
    }

    @Override
    public KnowledgeBaseVO getKnowledgeBase(String uuid, String userId) {
        log.info("获取知识库详情，uuid: {}, userId: {}", uuid, userId);

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUuid, uuid);
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(wrapper);

        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        // 权限检查
        if (!canAccessKb(userId, kb.getId())) {
            throw new SecurityException("无权访问该知识库");
        }

        return convertToVO(kb);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO updateKnowledgeBase(String uuid, KnowledgeBasePatchDTO dto, String userId) {
        log.info("更新知识库，uuid: {}, userId: {}", uuid, userId);

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUuid, uuid);
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(wrapper);

        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        // 权限检查：只有创建者可以修改
        if (!kb.getOwnerId().equals(userId)) {
            throw new SecurityException("无权修改该知识库");
        }

        // 更新字段（只更新非空字段）
        if (StringUtils.isNotBlank(dto.getName())) {
            kb.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            kb.setDescription(dto.getDescription());
        }
        if (dto.getIcon() != null) {
            kb.setIcon(dto.getIcon());
        }
        if (dto.getAccessLevel() != null) {
            kb.setAccessLevel(dto.getAccessLevel());
        }
        if (dto.getChunkSize() != null) {
            if (dto.getChunkSize() < 100 || dto.getChunkSize() > 2000) {
                throw new IllegalArgumentException("chunk_size必须在100-2000之间");
            }
            kb.setChunkSize(dto.getChunkSize());
        }
        if (dto.getChunkOverlap() != null) {
            kb.setChunkOverlap(dto.getChunkOverlap());
        }
        if (dto.getEmbeddingModelId() != null) {
            kb.setEmbeddingModelId(dto.getEmbeddingModelId());
        }
        if (dto.getRetrievalConfig() != null) {
            kb.setRetrievalConfig(dto.getRetrievalConfig());
        }

        knowledgeBaseMapper.updateById(kb);

        log.info("知识库更新成功，id: {}", kb.getId());
        return convertToVO(kb);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(String uuid, String userId) {
        log.info("删除知识库，uuid: {}, userId: {}", uuid, userId);

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUuid, uuid);
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(wrapper);

        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        // 权限检查：只有创建者可以删除
        if (!kb.getOwnerId().equals(userId)) {
            throw new SecurityException("无权删除该知识库");
        }

        // TODO: 级联删除关联的文档和向量数据
        // 这里需要调用文档服务来删除关联的文档

        knowledgeBaseMapper.deleteById(kb.getId());

        log.info("知识库删除成功，id: {}", kb.getId());
    }

    @Override
    public boolean canAccessKb(String userId, String kbId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            return false;
        }

        // 创建者可以访问
        if (kb.getOwnerId().equals(userId)) {
            return true;
        }

        // 公开知识库可以访问
        if ("public".equals(kb.getAccessLevel())) {
            return true;
        }

        // protected 级别需要额外的权限检查（这里简化处理）
        if ("protected".equals(kb.getAccessLevel())) {
            // TODO: 实现更复杂的权限检查逻辑
            return false;
        }

        // private 只有创建者可以访问
        return false;
    }

    /**
     * 将实体转换为VO
     */
    private KnowledgeBaseVO convertToVO(KnowledgeBase kb) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(kb, vo);
        return vo;
    }
}
