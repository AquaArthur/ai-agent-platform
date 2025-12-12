package org.demo.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.demo.core.model.entity.WorkflowExecution;

/**
 * 工作流执行历史Mapper接口
 * 继承MyBatis Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecution> {
    
}
