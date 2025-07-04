package com.jpmorgan.reactdemo.formatting.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaFormattingRules {
    private GlobalFormattingRule globalRules;
    private Map<String, CategoryFormattingRule> categoryRules;
    private Map<String, TypeFormattingRule> typeRules;
    private List<ConditionalFormattingRule> conditionalRules;

    public SchemaFormattingRules() {
        this.categoryRules = new HashMap<>();
        this.typeRules = new HashMap<>();
        this.conditionalRules = new ArrayList<>();
    }
}
