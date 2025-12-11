package org.demo.core.workflow.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.Workflow;
import org.demo.core.workflow.node.BaseNodeConfig;
import org.demo.core.workflow.node.EndNodeConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 结束节点执行器
 * 结束节点负责输出工作流执行结果
 */
@Slf4j
@Component
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) {
        log.info("执行结束节点: {}", node.getId());
        
        // 结束节点返回所有节点的输出作为最终结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("nodeOutputs", context.getNodeOutputs());
        
        return result;
    }

    @Override
    public String getSupportedType() {
        return "end";
    }
}
