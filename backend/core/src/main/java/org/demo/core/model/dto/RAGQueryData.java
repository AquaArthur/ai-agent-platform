package org.demo.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RAGQueryData {
    private int result_num;
    private List<RAGQueryResultItem> results;
}
