package org.demo.core.model.dto;

import lombok.AllArgsConstructor;
import  lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RAGQueryResultItem {
    private int vector_id;
    private int chunk_index;
    private double score;
    private String content;
}
