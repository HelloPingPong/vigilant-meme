package com.jpmorgan.reactdemo.dto;

import lombok.Data;
import java.util.List;

@Data
public class GenerationRequest {
    private List<FieldDefinitionDto> schema; // Use DTOs for incoming requests
    private int rowCount;
    private String format; // "CSV", "JSON", "SQL", "XML", "PLAINTEXT"
    private String tableName; // Optional: Needed for SQL format
    private String schemaFormattingRules; //JSON string for schema-level rules
}