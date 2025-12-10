package org.demo.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedData {
    private String knowledge_base_id;
    private String document_id;
    private int chunks;
    private int vector_dim;
}
