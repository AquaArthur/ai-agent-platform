package org.demo.core.service;

import org.demo.core.api.ApiResponse;
import org.demo.core.model.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RagClient {

    @Value("${rag.api.base-url}")
    private String baseUrl = "http://localhost:9000";   // 例如 http://localhost:9000

    private final RestTemplate restTemplate;

    public RagClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用 RAG 文档向量化 API
     *
     * @param kb_id    知识库ID
     * @param doc_id   文档ID
     * @param title    文档标题
     * @param content  文档内容
     * @return 包含向量化结果的响应对象
     */
    public EmbedDocResponse embedDocument(String kb_id,String doc_id,String title,String content) {
        EmbedDocRequest request = new EmbedDocRequest(kb_id, doc_id, title, content);
        String url = baseUrl + "/api/v1/rag/embed";

        try {
            return restTemplate.postForObject(url, request, EmbedDocResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("调用 RAG 文档向量化 API 失败: " + e.getMessage(), e);
        }
    }


    /**
     * 调用RAG查询API
     *
     * @param knowledgeBaseId     知识库ID
     * @param query               查询内容
     * @param topK                返回的最相似结果数量
     * @param similarityThreshold 相似度阈值（可选）
     */
    public ApiResponse<RAGQueryData> query(String knowledgeBaseId, String query, int topK, Double similarityThreshold) {
        return query(knowledgeBaseId, query, topK, similarityThreshold, "text-embedding-v4");
    }

    /**
     * 调用RAG查询API
     *
     * @param knowledgeBaseId 知识库ID
     * @param query           查询内容
     * @param topK            返回的最相似结果数量
     * @param similarityThreshold 相似度阈值（可选）
     * @param modelName      使用的模型名称（可选）
     */
    public ApiResponse<RAGQueryData> query(String knowledgeBaseId, String query, int topK, Double similarityThreshold, String modelName) {
        String url = baseUrl + "/api/v1/rag/query";
//        System.out.println("RagClient query url: "+ url);
        RAGQueryRequest request = new RAGQueryRequest(knowledgeBaseId, query, topK, similarityThreshold, modelName);
        ParameterizedTypeReference<ApiResponse<RAGQueryData>> responseType =
                new ParameterizedTypeReference<>() {};

        return restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(request),responseType).getBody();
    }

}

