package org.demo.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.demo.core.model.entity.Document;

/**
 * 文档 Mapper 接口
 * 继承 MyBatis Plus 的 BaseMapper，提供基础 CRUD 操作
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
