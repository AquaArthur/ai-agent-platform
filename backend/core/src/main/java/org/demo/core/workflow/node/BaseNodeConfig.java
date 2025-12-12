package org.demo.core.workflow.node;

import lombok.Data;

/**
 * 节点配置基类
 * 所有节点配置类都继承此类
 */
@Data
public abstract class BaseNodeConfig {
    /**
     * 节点类型
     */
    private String type;

    /**
     * 验证节点配置是否合法
     * 子类需要重写此方法实现具体验证逻辑
     * @throws IllegalArgumentException 配置不合法时抛出异常
     */
    public abstract void validate();
}
