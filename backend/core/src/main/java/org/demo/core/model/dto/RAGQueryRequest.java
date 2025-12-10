package org.demo.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RAGQueryRequest {
    private String knowledge_base_id;
    private String query;
    private int top_k = 5;
    private Double similarity_threshold = 0.5; // 可选
    private String model_name = "text-embedding-v4";   // 可选
}
