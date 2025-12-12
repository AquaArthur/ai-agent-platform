package org.demo.core.workflow.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.Workflow;
import org.demo.core.workflow.node.BaseNodeConfig;
import org.demo.core.workflow.node.StartNodeConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 开始节点执行器
 * 开始节点负责接收工作流输入参数
 */
@Slf4j
@Component
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) {
        log.info("执行开始节点: {}", node.getId());
        
        // 开始节点直接返回输入参数
        Map<String, Object> result = new HashMap<>();
        result.put("status", "started");
        result.put("input", context.getInput());
        
        return result;
    }

    @Override
    public String getSupportedType() {
        return "start";
    }
}
