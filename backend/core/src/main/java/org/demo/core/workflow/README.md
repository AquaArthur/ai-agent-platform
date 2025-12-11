# 工作流执行引擎实现文档

## 概述

本工作流执行引擎实现了基于节点的工作流系统，支持多种节点类型，使用线程池进行任务管理，节点串行执行。

## 架构设计

### 核心组件

1. **节点配置类（Node Config）**
   - `BaseNodeConfig`: 所有节点配置的基类
   - 各节点类型配置类：`StartNodeConfig`, `EndNodeConfig`, `LLMNodeConfig`, `HttpNodeConfig`, `KnowledgeNodeConfig`, `IntentNodeConfig`, `StringNodeConfig`
   - `NodeConfigFactory`: 根据节点类型创建对应的配置对象

2. **节点执行器（Node Executor）**
   - `NodeExecutor`: 节点执行器接口
   - 各节点类型执行器：`StartNodeExecutor`, `EndNodeExecutor`, `LLMNodeExecutor`, `HttpNodeExecutor`, `KnowledgeNodeExecutor`, `IntentNodeExecutor`, `StringNodeExecutor`

3. **变量解析器（Variable Resolver）**
   - `VariableResolver`: 支持 `{node_id}`, `{node_id.field}`, `{input.param}` 格式的变量替换

4. **工作流执行引擎（Workflow Executor）**
   - `WorkflowExecutor`: 使用线程池管理工作流执行任务，支持同步和异步执行
   - `ExecutionContext`: 工作流执行上下文，存储输入参数和节点输出
   - `NodeExecutionRecord`: 节点执行记录
   - `WorkflowExecutionResult`: 工作流执行结果

5. **工作流执行服务（Workflow Execution Service）**
   - `WorkflowExecutionService`: 负责工作流的启动、状态管理和结果记录

## 目录结构

```
org/demo/core/workflow/
├── node/                          # 节点配置包
│   ├── BaseNodeConfig.java        # 节点配置基类
│   ├── StartNodeConfig.java       # 开始节点配置
│   ├── EndNodeConfig.java         # 结束节点配置
│   ├── LLMNodeConfig.java         # LLM节点配置
│   ├── HttpNodeConfig.java        # HTTP节点配置
│   ├── KnowledgeNodeConfig.java   # 知识库节点配置
│   ├── IntentNodeConfig.java      # 意图识别节点配置
│   ├── StringNodeConfig.java      # 字符串处理节点配置
│   └── NodeConfigFactory.java     # 节点配置工厂
├── executor/                      # 执行器包
│   ├── NodeExecutor.java          # 节点执行器接口
│   ├── StartNodeExecutor.java     # 开始节点执行器
│   ├── EndNodeExecutor.java       # 结束节点执行器
│   ├── LLMNodeExecutor.java       # LLM节点执行器
│   ├── HttpNodeExecutor.java      # HTTP节点执行器
│   ├── KnowledgeNodeExecutor.java # 知识库节点执行器
│   ├── IntentNodeExecutor.java    # 意图识别节点执行器
│   ├── StringNodeExecutor.java    # 字符串处理节点执行器
│   ├── ExecutionContext.java      # 执行上下文
│   ├── NodeExecutionRecord.java   # 节点执行记录
│   ├── WorkflowExecutionResult.java # 工作流执行结果
│   └── WorkflowExecutor.java      # 工作流执行引擎
├── util/                          # 工具包
│   └── VariableResolver.java      # 变量解析器
└── service/                       # 服务包
    └── WorkflowExecutionService.java # 工作流执行服务
```

## 节点类型

### 1. 开始节点（start）
- 工作流的入口节点
- 接收工作流输入参数

### 2. 结束节点（end）
- 工作流的出口节点
- 输出工作流执行结果

### 3. LLM节点（llm）
- 调用大模型生成文本
- 配置项：
  - `agentUuid`: 智能体UUID（必填）
  - `prompt`: 提示词（必填，支持变量替换）
  - `temperature`: 温度参数（0-2，默认0.7）
  - `maxTokens`: 最大生成token数（默认2000）

### 4. HTTP请求节点（http）
- 调用外部HTTP API服务
- 配置项：
  - `url`: 请求URL（必填，支持变量替换）
  - `method`: 请求方法（GET/POST，默认GET）
  - `headers`: 请求头（支持变量替换）
  - `body`: 请求体（POST时使用，支持变量替换）

### 5. 知识库检索节点（knowledge）
- 从知识库中检索相关内容
- 配置项：
  - `knowledgeBaseId`: 知识库ID（必填）
  - `query`: 查询文本（必填，支持变量替换）
  - `topK`: 返回文档数（1-10，默认5）
  - `similarityThreshold`: 相似度阈值（0-1，默认0.7）

### 6. 意图识别节点（intent）
- 识别用户输入的意图
- 配置项：
  - `inputText`: 输入文本（必填，支持变量替换）
  - `intentCategories`: 意图类别列表（必填）
  - `recognitionMethod`: 识别方式（llm/keyword，默认llm）
  - `agentUuid`: 智能体UUID（llm方式时必填）
  - `keywords`: 关键词映射（keyword方式时必填）

### 7. 字符串处理节点（string）
- 处理字符串数据
- 支持的操作：
  - `concat`: 拼接字符串
  - `replace`: 替换字符串
  - `substring`: 截取字符串
  - `format`: 格式化字符串
  - `trim`: 去除首尾空格
  - `upper`: 转换为大写
  - `lower`: 转换为小写

## 变量替换

### 支持的变量格式

1. `{input.param}`: 引用工作流输入参数
2. `{node_id}`: 引用节点的完整输出
3. `{node_id.field}`: 引用节点输出的特定字段

### 示例

```json
{
  "type": "llm",
  "config": {
    "agentUuid": "xxx",
    "prompt": "用户说：{input.user_message}，之前的回答是：{llm_node_1.output}"
  }
}
```

## 工作流执行流程

1. **验证工作流**: 检查开始/结束节点、循环依赖、边有效性
2. **初始化上下文**: 创建执行上下文，存储输入参数
3. **构建执行顺序**: 从开始节点开始，按边的连接关系确定执行顺序
4. **串行执行节点**:
   - 创建节点配置
   - 替换配置中的变量
   - 执行节点
   - 保存节点输出到上下文
5. **记录执行结果**: 保存到数据库

## 线程池配置

- 核心线程数：5
- 最大线程数：10
- 队列容量：100
- 空闲线程存活时间：60秒
- 拒绝策略：CallerRunsPolicy（调用者运行）

## 使用示例

### 同步执行工作流

```java
@Autowired
private WorkflowExecutionService workflowExecutionService;

public void example() {
    String workflowId = "workflow-id";
    Map<String, Object> input = new HashMap<>();
    input.put("user_message", "你好");
    
    String executionId = workflowExecutionService.executeWorkflow(
        workflowId, 
        input, 
        "user-id",
        "llm-model-id"  // LLM模型ID，用于LLM和Intent节点
    );
    
    // 查询执行结果
    WorkflowExecution execution = workflowExecutionService.getExecution(executionId);
}
```

### 异步执行工作流

```java
@Autowired
private WorkflowExecutionService workflowExecutionService;

public void example() {
    String workflowId = "workflow-id";
    Map<String, Object> input = new HashMap<>();
    input.put("user_message", "你好");
    
    // 提交异步执行
    String executionId = workflowExecutionService.executeWorkflowAsync(
        workflowId, 
        input, 
        "user-id",
        "llm-model-id"  // LLM模型ID，用于LLM和Intent节点
    );
    
    // 稍后查询执行结果
    WorkflowExecution execution = workflowExecutionService.getExecution(executionId);
}
```

## 扩展指南

### 添加新的节点类型

1. 创建节点配置类，继承 `BaseNodeConfig`
2. 创建节点执行器，实现 `NodeExecutor` 接口
3. 在 `NodeConfigFactory` 中添加节点类型映射
4. 将执行器注册为Spring Bean（使用 `@Component` 注解）

### 示例：添加自定义节点

```java
// 1. 节点配置类
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomNodeConfig extends BaseNodeConfig {
    private String customParam;
    
    public CustomNodeConfig() {
        setType("custom");
    }
    
    @Override
    public void validate() {
        if (customParam == null) {
            throw new IllegalArgumentException("customParam不能为空");
        }
    }
}

// 2. 节点执行器
@Component
public class CustomNodeExecutor implements NodeExecutor {
    
    @Override
    public Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) {
        CustomNodeConfig customConfig = (CustomNodeConfig) config;
        // 执行自定义逻辑
        Map<String, Object> result = new HashMap<>();
        result.put("output", "result");
        return result;
    }
    
    @Override
    public String getSupportedType() {
        return "custom";
    }
}

// 3. 在NodeConfigFactory中添加
case "custom":
    nodeConfig = objectMapper.convertValue(config, CustomNodeConfig.class);
    break;
```

## 注意事项

1. **节点顺序**: 当前实现为串行执行，按边的连接关系顺序执行
2. **错误处理**: 节点执行失败时默认停止工作流执行
3. **变量作用域**: 节点只能访问前面节点的输出，不能访问后续节点
4. **线程安全**: 执行上下文在单个工作流执行中是线程安全的
5. **资源释放**: 工作流执行引擎在应用关闭时会自动关闭线程池

## 后续优化方向

1. 支持条件分支节点
2. 支持并行执行多个节点
3. 支持子工作流调用
4. 支持工作流执行的暂停和恢复
5. 支持工作流执行的实时状态查询
6. 支持节点执行超时控制
7. 支持节点执行重试机制
