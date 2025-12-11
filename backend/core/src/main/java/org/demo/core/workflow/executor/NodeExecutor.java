package org.demo.core.workflow.executor;

import org.demo.core.model.entity.Workflow;
import org.demo.core.workflow.node.BaseNodeConfig;

/**
 * 节点执行器接口
 * 定义节点执行的基本接口
 */
public interface NodeExecutor {

    /**
     * 执行节点
     *
     * @param node 工作流节点
     * @param config 节点配置
     * @param context 执行上下文
     * @return 节点执行结果
     * @throws Exception 执行失败时抛出异常
     */
    Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) throws Exception;

    /**
     * 获取支持的节点类型
     *
     * @return 节点类型
     */
    String getSupportedType();
}
