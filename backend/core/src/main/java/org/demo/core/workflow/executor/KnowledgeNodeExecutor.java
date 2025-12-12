package org.demo.core.workflow.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.core.api.ApiResponse;
import org.demo.core.model.dto.RAGQueryData;
import org.demo.core.model.dto.RAGQueryResultItem;
import org.demo.core.model.entity.Workflow;
import org.demo.core.service.RagClient;
import org.demo.core.workflow.node.BaseNodeConfig;
import org.demo.core.workflow.node.KnowledgeNodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索节点执行器
 * 从知识库中检索相关内容
 */
@Slf4j
@Component
public class KnowledgeNodeExecutor implements NodeExecutor {

    @Autowired
    private RagClient ragClient;

    @Override
    public Object execute(Workflow.WorkflowNode node, BaseNodeConfig config, ExecutionContext context) throws Exception {
        log.info("执行Knowledge节点: {}", node.getId());
        
        KnowledgeNodeConfig knowledgeConfig = (KnowledgeNodeConfig) config;
        
        // 调用RAG客户端进行检索
        ApiResponse<RAGQueryData> response = ragClient.query(
            String.valueOf(knowledgeConfig.getKnowledgeBaseId()),
            knowledgeConfig.getQuery(),
            knowledgeConfig.getTopK(),
            knowledgeConfig.getSimilarityThreshold()
        );
        
        List<Map<String, Object>> documents = new ArrayList<>();
        int resultNum = 0;
        
        if (response != null && response.getCode() == 200 && response.getData() != null) {
            resultNum = response.getData().getResult_num();
            if (resultNum > 0) {
                List<RAGQueryResultItem> results = response.getData().getResults();
                for (RAGQueryResultItem item : results) {
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("content", item.getContent());
                    doc.put("score", item.getScore());
                    doc.put("vector_id",  item.getVector_id());
                    doc.put("chunk_index", item.getChunk_index());
//                    doc.put("doc_id", item.getDoc_id());
                    documents.add(doc);
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("documents", documents);
        result.put("count", resultNum);
        result.put("query", knowledgeConfig.getQuery());
        
        return result;
    }

    @Override
    public String getSupportedType() {
        return "knowledge";
    }
}
