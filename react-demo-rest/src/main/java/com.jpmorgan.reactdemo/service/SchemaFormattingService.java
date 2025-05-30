package com.jpmorgan.reactdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpmorgan.reactdemo.dto.FieldDefinitionDto;
import com.jpmorgan.reactdemo.formatting.FieldFormattingOptions;
import com.jpmorgan.reactdemo.formatting.schema.CategoryFormattingRule;
import com.jpmorgan.reactdemo.formatting.schema.GlobalFormattingRule;
import com.jpmorgan.reactdemo.formatting.schema.SchemaFormattingRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.jpmorgan.reactdemo.service.DataGenerationService.log;

@Service
@RequiredArgsConstructor
public class SchemaFormattingService {

    private final ObjectMapper objectMapper;

    public SchemaFormattingRules parseSchemaRules(String rulesJson) {
        if (rulesJson == null || rulesJson.trim().isEmpty()) {
            return new SchemaFormattingRules();
        }

        try {
            return objectMapper.readValue(rulesJson, SchemaFormattingRules.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse schema formatting rules: {}", e.getMessage());
            return new SchemaFormattingRules();
        }
    }

    public FieldFormattingOptions resolveFieldFormatting(
            FieldDefinitionDto field,
            SchemaFormattingRules schemaRules,
            FieldFormattingOptions fieldLevelRules) {

        // Build formatting in order of precedence (lowest to highest):
        // 1. Global rules
        // 2. Category rules
        // 3. Type-specific rules
        // 4. Conditional rules
        // 5. Field-level rules (highest precedence)

        FieldFormattingOptions resolved = new FieldFormattingOptions();

        // Apply global rules
        if (schemaRules.getGlobalRules() != null) {
            applyGlobalRules(resolved, schemaRules.getGlobalRules());
        }

        // Apply category rules
        String category = extractCategory(field.getDataType());
        if (category != null && schemaRules.getCategoryRules().containsKey(category)) {
            CategoryFormattingRule categoryRule = schemaRules.getCategoryRules().get(category);
            if (!categoryRule.getOverrideFieldRules()) {
                mergeFormatting(resolved, categoryRule.getFormatting());
            }
        }

        // Apply type-specific rules
        if (schemaRules.getTypeRules().containsKey(field.getDataType())) {
            TypeFormattingRule typeRule = schemaRules.getTypeRules().get(field.getDataType());
            mergeFormatting(resolved, typeRule.getFormatting());
        }

        // Apply conditional rules
        applyConditionalRules(resolved, field, schemaRules.getConditionalRules());

        // Apply field-level rules (highest precedence)
        if (fieldLevelRules != null) {
            mergeFormatting(resolved, fieldLevelRules);
        }

        return resolved;
    }

    private void applyGlobalRules(FieldFormattingOptions target, GlobalFormattingRule global) {
        if (global.getDefaultCase() != null && target.getCaseTransform() == null) {
            target.setCaseTransform(global.getDefaultCase());
        }
        if (global.getDefaultPrefix() != null && target.getPrefix() == null) {
            target.setPrefix(global.getDefaultPrefix());
        }
        // ... apply other global rules
    }
}
