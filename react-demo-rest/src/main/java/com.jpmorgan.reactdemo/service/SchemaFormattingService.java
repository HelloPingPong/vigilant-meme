package com.jpmorgan.reactdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpmorgan.reactdemo.dto.FieldDefinitionDto;
import com.jpmorgan.reactdemo.formatting.FieldFormattingOptions;
import com.jpmorgan.reactdemo.formatting.FixedLengthConfig;
import com.jpmorgan.reactdemo.formatting.schema.CategoryFormattingRule;
import com.jpmorgan.reactdemo.formatting.schema.ConditionalFormattingRule;
import com.jpmorgan.reactdemo.formatting.schema.GlobalFormattingRule;
import com.jpmorgan.reactdemo.formatting.schema.SchemaFormattingRules;
import com.jpmorgan.reactdemo.formatting.schema.TypeFormattingRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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

        FieldFormattingOptions resolved = new FieldFormattingOptions();

        // 1. Apply global rules (lowest precedence)
        if (schemaRules.getGlobalRules() != null) {
            applyGlobalRules(resolved, schemaRules.getGlobalRules());
        }

        // 2. Apply category rules
        String category = extractCategory(field.getDataType());
        if (category != null && schemaRules.getCategoryRules().containsKey(category)) {
            CategoryFormattingRule categoryRule = schemaRules.getCategoryRules().get(category);
            if (!categoryRule.getOverrideFieldRules()) {
                mergeFormatting(resolved, categoryRule.getFormatting());
            }
        }

        // 3. Apply type-specific rules
        if (schemaRules.getTypeRules().containsKey(field.getDataType())) {
            TypeFormattingRule typeRule = schemaRules.getTypeRules().get(field.getDataType());
            mergeFormatting(resolved, typeRule.getFormatting());
        }

        // 4. Apply conditional rules
        applyConditionalRules(resolved, field, schemaRules.getConditionalRules());

        // 5. Apply field-level rules (highest precedence)
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
        if (global.getDefaultSuffix() != null && target.getSuffix() == null) {
            target.setSuffix(global.getDefaultSuffix());
        }
        if (global.getDefaultMaxLength() != null && target.getMaxLength() == null) {
            target.setMaxLength(global.getDefaultMaxLength());
        }
    }

    private String extractCategory(String dataType) {
        if (dataType == null || !dataType.contains(".")) {
            return null;
        }
        return dataType.substring(0, dataType.indexOf('.'));
    }

    private void mergeFormatting(FieldFormattingOptions target, FieldFormattingOptions source) {
        if (source == null) return;

        if (source.getCaseTransform() != null) {
            target.setCaseTransform(source.getCaseTransform());
        }
        if (source.getPrefix() != null) {
            target.setPrefix(source.getPrefix());
        }
        if (source.getSuffix() != null) {
            target.setSuffix(source.getSuffix());
        }
        if (source.getMaxLength() != null) {
            target.setMaxLength(source.getMaxLength());
        }
        if (source.getFixedLength() != null) {
            target.setFixedLength(source.getFixedLength());
        }
        if (source.getValidation() != null) {
            target.setValidation(source.getValidation());
        }
    }

    private void applyConditionalRules(FieldFormattingOptions target, FieldDefinitionDto field,
            List<ConditionalFormattingRule> conditionalRules) {
        if (conditionalRules == null || conditionalRules.isEmpty()) return;

        // Sort by priority (higher priority wins)
        conditionalRules.stream()
                .sorted(Comparator.comparing(ConditionalFormattingRule::getPriority).reversed())
                .forEach(rule -> {
                    if (evaluateCondition(rule.getCondition(), field)) {
                        mergeFormatting(target, rule.getFormatting());
                    }
                });
    }

    private boolean evaluateCondition(String condition, FieldDefinitionDto field) {
        if (condition == null || condition.trim().isEmpty()) return false;

        // Simple condition evaluation
        String fieldName = field.getName().toLowerCase();
        String dataType = field.getDataType().toLowerCase();
        condition = condition.toLowerCase();

        // Check field name conditions
        if (condition.contains("fieldname.contains")) {
            String searchText = extractQuotedText(condition);
            return searchText != null && fieldName.contains(searchText);
        }

        // Check data type conditions
        if (condition.contains("datatype.contains")) {
            String searchText = extractQuotedText(condition);
            return searchText != null && dataType.contains(searchText);
        }

        // Default: false
        return false;
    }

    private String extractQuotedText(String text) {
        int start = text.indexOf('\'');
        int end = text.lastIndexOf('\'');
        if (start >= 0 && end > start) {
            return text.substring(start + 1, end);
        }
        return null;
    }
}
