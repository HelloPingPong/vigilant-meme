package com.jpmorgan.reactdemo.formatting.schema;

import com.jpmorgan.reactdemo.formatting.FieldFormattingOptions;
import lombok.Data;

@Data
public class ConditionalFormattingRule {
    private String condition; // e.g., "fieldName.contains('email')"
    private FieldFormattingOptions formatting;
    private Integer priority = 0; // Higher priority wins
}
