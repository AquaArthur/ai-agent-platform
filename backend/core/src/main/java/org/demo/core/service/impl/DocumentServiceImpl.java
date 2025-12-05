package org.demo.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.demo.core.mapper.DocumentMapper;
import org.demo.core.mapper.KnowledgeBaseMapper;
import org.demo.core.model.entity.Document;
import org.demo.core.model.entity.KnowledgeBase;
import org.demo.core.model.vo.DocumentVO;
import org.demo.core.model.vo.PageResult;
import org.demo.core.service.DocumentService;
import org.demo.core.service.KnowledgeBaseService;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseService knowledgeBaseService;

    // 文件存储根目录（可配置）
    private static final String FILE_STORAGE_PATH = "data/documents/";
    // 最大文件大小 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    // 支持的文件类型
    private static final List<String> ALLOWED_FILE_TYPES = List.of("txt", "md", "markdown");

    @Override
    public PageResult<DocumentVO> listDocuments(String kbUuid, Integer page, Integer pageSize, String status, String userId) {
        log.info("分页查询文档列表，kbUuid: {}, page: {}, pageSize: {}, status: {}, userId: {}",
                kbUuid, page, pageSize, status, userId);

        // 获取知识库
        KnowledgeBase kb = getKnowledgeBaseByUuid(kbUuid);
        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        // 权限检查
        if (!knowledgeBaseService.canAccessKb(userId, kb.getId())) {
            throw new SecurityException("无权访问该知识库");
        }

        // 构建查询条件
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKnowledgeBaseId, kb.getId());

        // 状态筛选
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(Document::getStatus, status);
        }

        // 排序：按创建时间倒序
        wrapper.orderByDesc(Document::getCreatedAt);

        // 分页查询
        Page<Document> pageParam = new Page<>(page, pageSize);
        IPage<Document> pageResult = documentMapper.selectPage(pageParam, wrapper);

        // 转换为VO
        List<DocumentVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO uploadDocument(String kbUuid, MultipartFile file, String userId) {
        log.info("上传文档，kbUuid: {}, filename: {}, userId: {}", kbUuid, file.getOriginalFilename(), userId);

        // 获取知识库
        KnowledgeBase kb = getKnowledgeBaseByUuid(kbUuid);
        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        // 权限检查
        if (!knowledgeBaseService.canAccessKb(userId, kb.getId())) {
            throw new SecurityException("无权访问该知识库");
        }

        // 验证文件
        validateFile(file);

        // 保存文件
        String filename = file.getOriginalFilename();
        String fileType = getFileExtension(filename);
        String filePath = null;
        
        try {
            filePath = saveFile(file, kb.getId());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }

        // 创建文档实体
        Document document = new Document();
        document.setUuid(UUID.randomUUID().toString());
        document.setName(filename);
        document.setFilename(filename);
        // 使用反射设置 fileName 字段（别名字段）
        try {
            Field fileNameField = Document.class.getDeclaredField("fileName");
            fileNameField.setAccessible(true);
            fileNameField.set(document, filename);
        } catch (Exception e) {
            log.warn("设置 fileName 字段失败: {}", e.getMessage());
        }
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setFileType(fileType);
        document.setChunkCount(0);
        document.setStatus("uploading");
        document.setProcessStatus(0);
        document.setKnowledgeBaseId(kb.getId());
        document.setKbId(kb.getId());
        document.setUserId(userId);
        document.setCreatedAt(LocalDateTime.now());
        document.setCreateTime(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());

        documentMapper.insert(document);
        log.info("文档创建成功，id: {}, uuid: {}", document.getId(), document.getUuid());

        // 异步处理文档（解析、分块、向量化）
        processDocumentAsync(document.getId());

        return convertToVO(document);
    }

    @Override
    public DocumentVO getDocument(String uuid, String userId) {
        log.info("获取文档详情，uuid: {}, userId: {}", uuid, userId);

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getUuid, uuid);
        Document document = documentMapper.selectOne(wrapper);

        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }

        // 获取知识库进行权限检查
        KnowledgeBase kb = knowledgeBaseMapper.selectById(document.getKnowledgeBaseId());
        if (kb == null || !knowledgeBaseService.canAccessKb(userId, kb.getId())) {
            throw new SecurityException("无权访问该文档");
        }

        return convertToVO(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String uuid, String userId) {
        log.info("删除文档，uuid: {}, userId: {}", uuid, userId);

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getUuid, uuid);
        Document document = documentMapper.selectOne(wrapper);

        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }

        // 获取知识库进行权限检查
        KnowledgeBase kb = knowledgeBaseMapper.selectById(document.getKnowledgeBaseId());
        if (kb == null || !knowledgeBaseService.canAccessKb(userId, kb.getId())) {
            throw new SecurityException("无权删除该文档");
        }

        // 删除物理文件
        if (StringUtils.isNotBlank(document.getFilePath())) {
            try {
                Path path = Paths.get(document.getFilePath());
                Files.deleteIfExists(path);
                log.info("物理文件删除成功: {}", document.getFilePath());
            } catch (IOException e) {
                log.error("物理文件删除失败: {}", document.getFilePath(), e);
            }
        }

        // TODO: 级联删除文档块和向量数据
        // db.query(DocumentChunk).filter(DocumentChunk.document_id == document.id).delete()

        // 删除文档记录
        documentMapper.deleteById(document.getId());
        log.info("文档删除成功，id: {}", document.getId());
    }

    @Override
    @Async
    public void processDocumentAsync(String documentId) {
        log.info("开始异步处理文档，documentId: {}", documentId);

        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            log.error("文档不存在，无法处理，documentId: {}", documentId);
            return;
        }

        try {
            // 更新状态为处理中
            document.setStatus("processing");
            document.setProcessStatus(1);
            document.setUpdatedAt(LocalDateTime.now());
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);

            // TODO: 实现文档处理逻辑
            // 1. 读取文件内容（UTF-8编码）
            // 2. 文档分块
            // 3. 向量化
            // 4. 存储到向量数据库

            // 模拟处理
            Thread.sleep(2000);

            // 更新状态为已完成
            document.setStatus("processed");
            document.setProcessStatus(2);
            document.setChunkCount(10); // 示例值
            document.setProcessedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);

            log.info("文档处理成功，documentId: {}", documentId);

        } catch (Exception e) {
            log.error("文档处理失败，documentId: {}", documentId, e);

            // 更新状态为失败
            document.setStatus("failed");
            document.setProcessStatus(3);
            document.setErrorMessage(e.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (StringUtils.isBlank(filename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 验证文件类型
        String fileType = getFileExtension(filename);
        if (!ALLOWED_FILE_TYPES.contains(fileType.toLowerCase())) {
            throw new IllegalArgumentException("仅支持TXT和Markdown格式，当前文件类型: " + fileType);
        }

        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }
    }

    /**
     * 保存文件到磁盘
     */
    private String saveFile(MultipartFile file, String kbId) throws IOException {
        // 创建存储目录
        String dirPath = FILE_STORAGE_PATH + kbId + "/";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + "." + fileExtension;
        String filePath = dirPath + newFilename;

        // 保存文件
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());

        log.info("文件保存成功: {}", filePath);
        return filePath;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 根据UUID获取知识库
     */
    private KnowledgeBase getKnowledgeBaseByUuid(String uuid) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUuid, uuid);
        return knowledgeBaseMapper.selectOne(wrapper);
    }

    /**
     * 转换为VO
     */
    private DocumentVO convertToVO(Document document) {
        if (document == null) {
            return null;
        }

        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);
        vo.setUuid(document.getUuid());
        vo.setName(document.getName());
        vo.setFilename(document.getFilename());
        vo.setFileUrl(document.getFileUrl());
        vo.setFileSize(document.getFileSize());
        vo.setFileType(document.getFileType());
        vo.setChunkCount(document.getChunkCount());
        vo.setStatus(document.getStatus());
        vo.setErrorMessage(document.getErrorMessage());
        vo.setProcessedAt(document.getProcessedAt());
        vo.setKnowledgeBaseId(document.getKnowledgeBaseId());
        vo.setUserId(document.getUserId());
        vo.setCreatedAt(document.getCreatedAt());
        vo.setUpdatedAt(document.getUpdatedAt());

        return vo;
    }
}
