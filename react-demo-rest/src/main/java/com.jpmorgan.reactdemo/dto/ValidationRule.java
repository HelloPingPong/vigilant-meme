package com.jpmorgan.reactdemo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ValidationRule {
    private Integer minLength;
    private Integer maxLength;
    private String pattern; // Regex pattern for validation
    private List<String> customRules; // e.g., "not_empty", "no_spaces", "alphanumeric"
}
