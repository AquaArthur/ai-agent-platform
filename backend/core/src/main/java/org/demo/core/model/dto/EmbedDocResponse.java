package org.demo.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedDocResponse {
    private int code;
    private String message;
    private EmbedData data;
    private long timestamp;
}