package com.jpmorgan.reactdemo.formatting.schema;

import com.jpmorgan.reactdemo.formatting.FieldFormattingOptions;
import lombok.Data;

@Data
public class CategoryFormattingRule {
    private String category; // e.g., "Name", "Address", "Internet"
    private FieldFormattingOptions formatting;
    private Boolean overrideFieldRules = false; // Whether to override individual field rules
}
