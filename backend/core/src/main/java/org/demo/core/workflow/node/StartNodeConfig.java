package org.demo.core.workflow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开始节点配置
 * 开始节点是工作流的入口，接收工作流输入参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StartNodeConfig extends BaseNodeConfig {

    public StartNodeConfig() {
        setType("start");
    }

    @Override
    public void validate() {
        // 开始节点无需特殊验证
    }
}
