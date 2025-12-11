package org.demo.core.workflow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 结束节点配置
 * 结束节点是工作流的出口，输出工作流执行结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EndNodeConfig extends BaseNodeConfig {

    public EndNodeConfig() {
        setType("end");
    }

    @Override
    public void validate() {
        // 结束节点无需特殊验证
    }
}
