package com.jpmorgan.reactdemo.dto;

import lombok.Data;

// DTO to decouple API from JPA Entity
@Data
public class FieldDefinitionDto {
    private String name;
    private String dataType;
    private String options;
    // No 'id' or 'schemaDefinition' needed for generation request
}
