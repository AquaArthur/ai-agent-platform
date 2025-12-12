package org.demo.core.workflow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 字符串处理节点配置
 * 处理字符串数据（拼接、替换、格式化等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StringNodeConfig extends BaseNodeConfig {
    /**
     * 操作类型
     * 支持：concat（拼接）、replace（替换）、substring（截取）、
     *      format（格式化）、trim（去空格）、upper（转大写）、lower（转小写）
     */
    private String operation;

    /**
     * 输入字符串，支持变量替换（必填）
     */
    private String inputString;

    /**
     * 操作参数，根据不同的操作类型而不同
     */
    private Map<String, Object> parameters;

    public StringNodeConfig() {
        setType("string");
    }

    @Override
    public void validate() {
        if (operation == null || operation.trim().isEmpty()) {
            throw new IllegalArgumentException("String节点的 operation 不能为空");
        }
        if (inputString == null) {
            throw new IllegalArgumentException("String节点的 inputString 不能为空");
        }
        
        // 验证操作类型
        String[] validOperations = {"concat", "replace", "substring", "format", "trim", "upper", "lower"};
        boolean validOperation = false;
        for (String validOp : validOperations) {
            if (validOp.equals(operation)) {
                validOperation = true;
                break;
            }
        }
        if (!validOperation) {
            throw new IllegalArgumentException("String节点的 operation 不支持，只支持: concat, replace, substring, format, trim, upper, lower");
        }
    }
}
