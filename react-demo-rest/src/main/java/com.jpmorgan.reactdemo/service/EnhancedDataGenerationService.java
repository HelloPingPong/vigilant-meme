package com.jpmorgan.reactdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.javafaker.Faker;
import com.jpmorgan.reactdemo.dto.EnhancedFieldOptions;
import com.jpmorgan.reactdemo.dto.FieldDefinitionDto;
import com.jpmorgan.reactdemo.dto.GenerationRequest;
import com.jpmorgan.reactdemo.expression.DependencyResolver;
import com.jpmorgan.reactdemo.expression.ExpressionEvaluator;
import com.jpmorgan.reactdemo.formatting.FieldFormatter;
import com.jpmorgan.reactdemo.formatting.FieldFormattingOptions;
import com.jpmorgan.reactdemo.formatting.schema.SchemaFormattingRules;
import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Primary
@Slf4j
public class EnhancedDataGenerationService extends DataGenerationService {

    private final SchemaFormattingService schemaFormattingService;
    private final FieldFormatter fieldFormatter;
    private final DependencyResolver dependencyResolver;
    private final ExpressionEvaluator expressionEvaluator;
    private final ObjectMapper objectMapper;

    public EnhancedDataGenerationService(
            List<DataTypeGenerator> generators,
            ObjectMapper objectMapper,
            CsvMapper csvMapper,
            XmlMapper xmlMapper,
            SchemaFormattingService schemaFormattingService,
            FieldFormatter fieldFormatter,
            DependencyResolver dependencyResolver,
            ExpressionEvaluator expressionEvaluator) {
        super(generators, objectMapper, csvMapper, xmlMapper);
        this.objectMapper = objectMapper;
        this.schemaFormattingService = schemaFormattingService;
        this.fieldFormatter = fieldFormatter;
        this.dependencyResolver = dependencyResolver;
        this.expressionEvaluator = expressionEvaluator;
    }

    @PostConstruct
    @Override
    public void init() {
        super.init(); // Initialize parent's generator map
        log.info("Enhanced data generation service initialized");
    }

    @Override
    public String generateData(GenerationRequest request) throws Exception {
        log.info("Enhanced generation for {} rows", request.getRowCount());

        // Parse schema-level formatting rules
        SchemaFormattingRules schemaRules = parseSchemaFormattingRules(request.getSchemaFormattingRules());

        // Generate with enhanced formatting
        List<Map<String, Object>> data = generateRawDataWithFormatting(request.getSchema(), request.getRowCount(), schemaRules);

        // Use parent's format method
        return formatData(data, request.getFormat(), request.getSchema(), request.getTableName());
    }

    @Override
    public List<Map<String, Object>> generateRawData(List<FieldDefinitionDto> schema, int rowCount) {
        // Enhanced version with default formatting
        return generateRawDataWithFormatting(schema, rowCount, new SchemaFormattingRules());
    }

    /**
     * Generate raw data with schema formatting rules (for preview endpoint)
     */
    public List<Map<String, Object>> generateRawDataWithSchemaRules(
            List<FieldDefinitionDto> schema,
            int rowCount,
            String schemaFormattingRulesJson) {
        SchemaFormattingRules schemaRules = parseSchemaFormattingRules(schemaFormattingRulesJson);
        return generateRawDataWithFormatting(schema, rowCount, schemaRules);
    }

    private List<Map<String, Object>> generateRawDataWithFormatting(
            List<FieldDefinitionDto> schema,
            int rowCount,
            SchemaFormattingRules schemaRules) {

        // Resolve dependencies
        List<FieldDefinitionDto> orderedSchema = dependencyResolver.resolveDependencyOrder(schema);

        return IntStream.range(0, rowCount)
                .mapToObj(i -> generateRowWithFormatting(orderedSchema, schemaRules, i))
                .collect(Collectors.toList());
    }

    private Map<String, Object> generateRowWithFormatting(
            List<FieldDefinitionDto> schema,
            SchemaFormattingRules schemaRules,
            int rowIndex) {

        Map<String, Object> row = new LinkedHashMap<>();
        Map<String, Object> generationContext = new HashMap<>();

        generationContext.put("_rowIndex", rowIndex);
        generationContext.put("_timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));

        for (FieldDefinitionDto field : schema) {
            try {
                // Parse enhanced options
                EnhancedFieldOptions fieldOptions = parseEnhancedFieldOptions(field.getOptions());

                // Get generator
                String generatorKey = fieldOptions.getBaseType() != null ? fieldOptions.getBaseType() : field.getDataType();
                DataTypeGenerator generator = generatorMap.get(generatorKey);

                if (generator == null) {
                    log.warn("No generator for type: {}", generatorKey);
                    row.put(field.getName(), "[NO_GENERATOR]");
                    continue;
                }

                // Generate value
                String value;
                if (fieldOptions.getDependency() != null && fieldOptions.getDependency().getExpression() != null) {
                    // Evaluate expression
                    value = expressionEvaluator.evaluateExpression(
                            fieldOptions.getDependency().getExpression(),
                            row,
                            generationContext,
                            faker
                    );
                } else {
                    // Use generator
                    String baseOptions = fieldOptions.getBaseOptions() != null ?
                            fieldOptions.getBaseOptions().toString() : field.getOptions();
                    value = generator.generate(faker, baseOptions, row);
                }

                // Apply formatting
                FieldFormattingOptions formatting = schemaFormattingService.resolveFieldFormatting(
                        field, schemaRules, fieldOptions.getFormatting()
                );
                value = fieldFormatter.applyFormatting(value, formatting);

                row.put(field.getName(), value);

            } catch (Exception e) {
                log.error("Error generating field '{}': {}", field.getName(), e.getMessage());
                row.put(field.getName(), "[ERROR]");
            }
        }

        return row;
    }

    private SchemaFormattingRules parseSchemaFormattingRules(String rulesJson) {
        if (rulesJson == null || rulesJson.trim().isEmpty()) {
            return new SchemaFormattingRules();
        }

        try {
            return objectMapper.readValue(rulesJson, SchemaFormattingRules.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse schema rules: {}", e.getMessage());
            return new SchemaFormattingRules();
        }
    }

    private EnhancedFieldOptions parseEnhancedFieldOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return new EnhancedFieldOptions();
        }

        try {
            // Try parsing as enhanced JSON
            return objectMapper.readValue(optionsJson, EnhancedFieldOptions.class);
        } catch (JsonProcessingException e) {
            // Fallback: treat as simple options
            EnhancedFieldOptions options = new EnhancedFieldOptions();
            options.setBaseOptions(optionsJson);
            return options;
        }
    }
}
