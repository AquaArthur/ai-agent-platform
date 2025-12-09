package org.demo.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedDocRequest {
    private String knowledge_base_id;
    private String document_id;
    private String title;
    private String content;
}


