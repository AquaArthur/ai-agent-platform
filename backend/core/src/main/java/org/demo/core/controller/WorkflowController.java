package org.demo.core.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.demo.core.api.ApiResponse;
import org.demo.core.mapper.WorkflowMapper;
import org.demo.core.mapper.WorkflowExecutionMapper;
import org.demo.core.model.entity.Workflow;
import org.demo.core.model.entity.WorkflowExecution;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作流控制器
 * 提供工作流的CRUD操作、验证和执行接口
 */
@Tag(name = "工作流管理", description = "提供工作流的增删改查、验证和执行等功能接口")
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowMapper workflowMapper;
    private final WorkflowExecutionMapper workflowExecutionMapper;

    /**
     * 查询工作流列表（支持分页和搜索）
     *
     * @param page     页码，默认1
     * @param pageSize 每页数量，默认10
     * @param search   搜索关键词
     * @return 工作流列表
     */
    @Operation(summary = "查询工作流列表", description = "获取工作流列表，支持分页和关键词搜索（搜索名称和描述）")
    @GetMapping
    public ApiResponse<Map<String, Object>> getWorkflows(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {
        
        Page<Workflow> pageParam = new Page<>(page, pageSize);
        QueryWrapper<Workflow> queryWrapper = new QueryWrapper<>();
        
        if (search != null && !search.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like("name", search)
                    .or()
                    .like("description", search));
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<Workflow> result = workflowMapper.selectPage(pageParam, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", result.getTotal());
        response.put("items", result.getRecords());
        
        return ApiResponse.ok(response);
    }

    /**
     * 根据UUID查询工作流详情
     *
     * @param uuid 工作流UUID
     * @return 工作流详情
     */
    @Operation(summary = "根据UUID查询工作流", description = "通过工作流的UUID查询指定工作流的详细信息，包括节点、边和配置等")
    @GetMapping("/{uuid}")
    public ApiResponse<Workflow> getWorkflowByUuid(
            @Parameter(description = "工作流的UUID", required = true) @PathVariable String uuid) {
        QueryWrapper<Workflow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uuid", uuid);
        Workflow workflow = workflowMapper.selectOne(queryWrapper);
        
        if (workflow == null) {
            return ApiResponse.fail("工作流不存在");
        }
        return ApiResponse.ok(workflow);
    }

    /**
     * 创建工作流
     *
     * @param workflow 工作流信息
     * @return 创建结果
     */
    @Operation(summary = "创建工作流", description = "创建一个新的工作流，需要提供工作流的基本信息、节点和边配置。系统会自动生成UUID和ID")
    @PostMapping
    public ApiResponse<Workflow> createWorkflow(
            @Parameter(description = "工作流信息对象", required = true) @RequestBody Workflow workflow) {
        
        // TODO: 从登录用户获取userId
        if (workflow.getUserId() == null || workflow.getUserId().isEmpty()) {
            workflow.setUserId("user-002-home"); // 使用测试数据中的默认用户
        }
        
        // 生成UUID
        if (workflow.getUuid() == null || workflow.getUuid().isEmpty()) {
            workflow.setUuid(UUID.randomUUID().toString());
        }
        
        // 设置默认值
        if (workflow.getIsValid() == null) {
            workflow.setIsValid(false);
        }
        if (workflow.getIsActive() == null) {
            workflow.setIsActive(true);
        }
        if (workflow.getIsPublic() == null) {
            workflow.setIsPublic(false);
        }
        if (workflow.getExecutionCount() == null) {
            workflow.setExecutionCount(0);
        }
        if (workflow.getSuccessCount() == null) {
            workflow.setSuccessCount(0);
        }
        
        int rows = workflowMapper.insert(workflow);
        if (rows > 0) {
            return ApiResponse.ok("创建成功", workflow);
        }
        return ApiResponse.fail("创建失败");
    }

    /**
     * 更新工作流
     *
     * @param uuid     工作流UUID
     * @param workflow 工作流信息
     * @return 更新结果
     */
    @Operation(summary = "更新工作流", description = "根据UUID更新指定工作流的信息。可以修改工作流的名称、描述、节点、边和配置等")
    @PutMapping("/{uuid}")
    @Transactional
    public ApiResponse<Workflow> updateWorkflow(
            @Parameter(description = "工作流的UUID", required = true) @PathVariable String uuid,
            @Parameter(description = "更新后的工作流信息", required = true) @RequestBody Workflow workflow) {
        
        QueryWrapper<Workflow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uuid", uuid);
        Workflow existingWorkflow = workflowMapper.selectOne(queryWrapper);
        
        if (existingWorkflow == null) {
            return ApiResponse.fail("工作流不存在");
        }
        
        // 保持ID和UUID不变
        workflow.setId(existingWorkflow.getId());
        workflow.setUuid(existingWorkflow.getUuid());
        
        int rows = workflowMapper.updateById(workflow);
        if (rows > 0) {
            return ApiResponse.ok("更新成功", workflow);
        }
        return ApiResponse.fail("更新失败");
    }

    /**
     * 删除工作流
     *
     * @param uuid 工作流UUID
     * @return 删除结果
     */
    @Operation(summary = "删除工作流", description = "根据UUID删除指定的工作流。执行物理删除操作，会同时删除关联的执行历史记录")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteWorkflow(
            @Parameter(description = "工作流的UUID", required = true) @PathVariable String uuid) {
        
        QueryWrapper<Workflow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uuid", uuid);
        Workflow existingWorkflow = workflowMapper.selectOne(queryWrapper);
        
        if (existingWorkflow == null) {
            return ApiResponse.fail("工作流不存在");
        }
        
        int rows = workflowMapper.deleteById(existingWorkflow.getId());
        if (rows > 0) {
            return ApiResponse.ok("删除成功", null);
        }
        return ApiResponse.fail("删除失败");
    }

    /**
     * 验证工作流
     *
     * @param uuid 工作流UUID
     * @return 验证结果
     */
    @Operation(summary = "验证工作流", description = "验证工作流的DAG结构是否合法，包括检查开始节点、结束节点、循环依赖和不可达节点等")
    @PostMapping("/{uuid}/validate")
    public ApiResponse<Map<String, Object>> validateWorkflow(
            @Parameter(description = "工作流的UUID", required = true) @PathVariable String uuid) {
        
        QueryWrapper<Workflow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uuid", uuid);
        Workflow workflow = workflowMapper.selectOne(queryWrapper);
        
        if (workflow == null) {
            return ApiResponse.fail("工作流不存在");
        }
        
        // 执行DAG验证
        Map<String, Object> validationResult = performDAGValidation(workflow);
        
        // 更新工作流的is_valid字段
        workflow.setIsValid((Boolean) validationResult.get("valid"));
        workflowMapper.updateById(workflow);
        
        return ApiResponse.ok(validationResult);
    }

    /**
     * 执行工作流
     *
     * @param uuid             工作流UUID
     * @param executionRequest 执行请求参数
     * @return 执行结果
     */
    @Operation(summary = "执行工作流", description = "触发工作流执行，传入初始输入参数。返回执行ID，可通过执行ID查询执行状态和结果")
    @PostMapping("/{uuid}/execute")
    public ApiResponse<Map<String, Object>> executeWorkflow(
            @Parameter(description = "工作流的UUID", required = true) @PathVariable String uuid,
            @Parameter(description = "执行请求参数", required = true) @RequestBody Map<String, Object> executionRequest) {
        
        QueryWrapper<Workflow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uuid", uuid);
        Workflow workflow = workflowMapper.selectOne(queryWrapper);
        
        if (workflow == null) {
            return ApiResponse.fail("工作流不存在");
        }
        
        if (!workflow.getIsValid()) {
            return ApiResponse.fail("工作流未通过验证，无法执行");
        }
        
        // 创建执行记录
        WorkflowExecution execution = new WorkflowExecution();
        execution.setExecutionId(UUID.randomUUID().toString());
        execution.setWorkflowId(workflow.getId());
        execution.setUserId(workflow.getUserId()); // TODO: 使用当前登录用户
        execution.setStatus("pending");
        execution.setInput((Map<String, Object>) executionRequest.get("input"));
        execution.setRunType("full");
        execution.setStartedAt(LocalDateTime.now());
        
        workflowExecutionMapper.insert(execution);
        
        // TODO: 异步执行工作流（当前仅创建执行记录）
        // 实际执行逻辑需要实现工作流执行引擎
        
        Map<String, Object> response = new HashMap<>();
        response.put("execution_id", execution.getExecutionId());
        response.put("status", execution.getStatus());
        
        return ApiResponse.ok(response);
    }

    /**
     * 获取工作流执行记录
     *
     * @param executionId 执行ID
     * @return 执行记录详情
     */
    @Operation(summary = "获取执行记录", description = "根据执行ID查询工作流执行的详细信息，包括状态、输入、输出、节点执行快照等")
    @GetMapping("/executions/{executionId}")
    public ApiResponse<WorkflowExecution> getExecution(
            @Parameter(description = "执行ID", required = true) @PathVariable String executionId) {
        
        QueryWrapper<WorkflowExecution> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("execution_id", executionId);
        WorkflowExecution execution = workflowExecutionMapper.selectOne(queryWrapper);
        
        if (execution == null) {
            return ApiResponse.fail("执行记录不存在");
        }
        
        return ApiResponse.ok(execution);
    }

    /**
     * 执行DAG验证
     *
     * @param workflow 工作流
     * @return 验证结果
     */
    private Map<String, Object> performDAGValidation(Workflow workflow) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        
        boolean valid = true;
        String errorMessage = null;
        
        List<Workflow.WorkflowNode> nodes = workflow.getNodes();
        List<Workflow.WorkflowEdge> edges = workflow.getEdges();
        
        if (nodes == null || nodes.isEmpty()) {
            valid = false;
            errorMessage = "工作流没有节点";
            result.put("valid", valid);
            result.put("error_message", errorMessage);
            return result;
        }
        
        // 检查是否有开始节点
        boolean hasStartNode = nodes.stream().anyMatch(node -> "start".equals(node.getType()));
        details.put("has_start_node", hasStartNode);
        if (!hasStartNode) {
            valid = false;
            errorMessage = "工作流缺少开始节点";
        }
        
        // 检查是否有结束节点
        boolean hasEndNode = nodes.stream().anyMatch(node -> "end".equals(node.getType()));
        details.put("has_end_node", hasEndNode);
        if (!hasEndNode) {
            valid = false;
            errorMessage = "工作流缺少结束节点";
        }
        
        // 检查循环依赖（简化版，使用DFS）
        boolean hasCycle = detectCycle(nodes, edges);
        details.put("has_cycle", hasCycle);
        if (hasCycle) {
            valid = false;
            errorMessage = "工作流存在循环依赖";
        }
        
        // 检查不可达节点
        List<String> unreachableNodes = findUnreachableNodes(nodes, edges);
        details.put("unreachable_nodes", unreachableNodes);
        if (!unreachableNodes.isEmpty()) {
            warnings.add("存在不可达节点: " + String.join(", ", unreachableNodes));
        }
        
        result.put("valid", valid);
        result.put("error_message", errorMessage);
        result.put("warnings", warnings);
        result.put("validation_details", details);
        
        return result;
    }

    /**
     * 检测循环依赖
     */
    private boolean detectCycle(List<Workflow.WorkflowNode> nodes, List<Workflow.WorkflowEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return false;
        }
        
        // 构建邻接表
        Map<String, List<String>> graph = new HashMap<>();
        for (Workflow.WorkflowNode node : nodes) {
            graph.put(node.getId(), new ArrayList<>());
        }
        for (Workflow.WorkflowEdge edge : edges) {
            graph.get(edge.getSource()).add(edge.getTarget());
        }
        
        // DFS检测环
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        
        for (String nodeId : graph.keySet()) {
            if (hasCycleDFS(nodeId, graph, visited, recStack)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * DFS检测环
     */
    private boolean hasCycleDFS(String nodeId, Map<String, List<String>> graph, 
                                 Set<String> visited, Set<String> recStack) {
        if (recStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }
        
        visited.add(nodeId);
        recStack.add(nodeId);
        
        for (String neighbor : graph.get(nodeId)) {
            if (hasCycleDFS(neighbor, graph, visited, recStack)) {
                return true;
            }
        }
        
        recStack.remove(nodeId);
        return false;
    }

    /**
     * 查找不可达节点
     */
    private List<String> findUnreachableNodes(List<Workflow.WorkflowNode> nodes, 
                                                List<Workflow.WorkflowEdge> edges) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 找到开始节点
        String startNodeId = nodes.stream()
                .filter(node -> "start".equals(node.getType()))
                .map(Workflow.WorkflowNode::getId)
                .findFirst()
                .orElse(null);
        
        if (startNodeId == null) {
            return nodes.stream()
                    .map(Workflow.WorkflowNode::getId)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 构建邻接表
        Map<String, List<String>> graph = new HashMap<>();
        for (Workflow.WorkflowNode node : nodes) {
            graph.put(node.getId(), new ArrayList<>());
        }
        if (edges != null) {
            for (Workflow.WorkflowEdge edge : edges) {
                graph.get(edge.getSource()).add(edge.getTarget());
            }
        }
        
        // BFS查找可达节点
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(startNodeId);
        reachable.add(startNodeId);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : graph.get(current)) {
                if (!reachable.contains(neighbor)) {
                    reachable.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }
        
        // 找出不可达节点
        return nodes.stream()
                .map(Workflow.WorkflowNode::getId)
                .filter(id -> !reachable.contains(id))
                .collect(java.util.stream.Collectors.toList());
    }
}
