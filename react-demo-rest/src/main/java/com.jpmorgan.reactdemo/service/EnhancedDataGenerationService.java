package com.jpmorgan.reactdemo.service;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedDataGenerationService {

    private final List<DataTypeGenerator> dataTypeGenerators;
    private final SchemaFormattingService schemaFormattingService;
    private final FieldFormatter fieldFormatter;
    private final DependencyResolver dependencyResolver;
    private final ExpressionEvaluator expressionEvaluator;
    private final ObjectMapper objectMapper;
    private final Faker faker = new Faker();

    /**
     * Main entry point for data generation with enhanced formatting support
     */
    public String generateData(GenerationRequest request) throws Exception {
        log.info("Starting data generation for {} rows in {} format",
                request.getRowCount(), request.getFormat());

        // Parse schema-level formatting rules
        SchemaFormattingRules schemaRules = parseSchemaFormattingRules(request.getSchemaFormattingRules());

        // Validate and prepare schema
        List<FieldDefinitionDto> validatedSchema = validateAndPrepareSchema(request.getSchema());

        // Generate raw data with all formatting applied
        List<Map<String, Object>> data = generateRawDataWithEnhancedFormatting(
                validatedSchema,
                request.getRowCount(),
                schemaRules
        );

        // Format output according to requested format
        return formatOutput(data, request.getFormat(), validatedSchema, request.getTableName());
    }

    /**
     * Generate preview data (limited rows for UI preview)
     */
    public List<Map<String, Object>> generatePreviewData(
            List<FieldDefinitionDto> schema,
            int rowCount,
            String schemaFormattingRulesJson) throws Exception {

        log.debug("Generating preview data for {} rows", rowCount);

        SchemaFormattingRules schemaRules = parseSchemaFormattingRules(schemaFormattingRulesJson);
        List<FieldDefinitionDto> validatedSchema = validateAndPrepareSchema(schema);

        return generateRawDataWithEnhancedFormatting(validatedSchema, rowCount, schemaRules);
    }

    /**
     * Parse schema-level formatting rules from JSON string
     */
    private SchemaFormattingRules parseSchemaFormattingRules(String rulesJson) {
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

    /**
     * Validate schema and prepare enhanced field options
     */
    private List<FieldDefinitionDto> validateAndPrepareSchema(List<FieldDefinitionDto> schema) {
        List<FieldDefinitionDto> validatedSchema = new ArrayList<>();

        for (FieldDefinitionDto field : schema) {
            if (field.getName() == null || field.getName().trim().isEmpty()) {
                log.warn("Skipping field with empty name");
                continue;
            }

            if (field.getDataType() == null || field.getDataType().trim().isEmpty()) {
                log.warn("Skipping field '{}' with empty data type", field.getName());
                continue;
            }

            // Validate that the data type generator exists
            if (getGenerator(field.getDataType()) == null) {
                log.warn("Unknown data type '{}' for field '{}', using default string generator",
                        field.getDataType(), field.getName());
                field.setDataType("String.random");
            }

            validatedSchema.add(field);
        }

        if (validatedSchema.isEmpty()) {
            throw new IllegalArgumentException("Schema contains no valid fields");
        }

        return validatedSchema;
    }

    /**
     * Generate raw data with full enhanced formatting support
     */
    private List<Map<String, Object>> generateRawDataWithEnhancedFormatting(
            List<FieldDefinitionDto> schema,
            int rowCount,
            SchemaFormattingRules schemaRules) {

        // Resolve field dependencies for proper generation order
        List<FieldDefinitionDto> orderedSchema = dependencyResolver.resolveDependencyOrder(schema);

        return IntStream.range(0, rowCount)
                .parallel() // Use parallel processing for better performance
                .mapToObj(i -> generateSingleRowWithFormatting(orderedSchema, schemaRules, i))
                .collect(Collectors.toList());
    }

    /**
     * Generate a single row with complete formatting and dependency support
     */
    private Map<String, Object> generateSingleRowWithFormatting(
            List<FieldDefinitionDto> orderedSchema,
            SchemaFormattingRules schemaRules,
            int rowIndex) {

        Map<String, Object> row = new LinkedHashMap<>();
        Map<String, Object> generationContext = new HashMap<>();

        // Add row index and metadata to context
        generationContext.put("_rowIndex", rowIndex);
        generationContext.put("_timestamp", LocalDateTime.now());

        for (FieldDefinitionDto field : orderedSchema) {
            try {
                // Parse enhanced field options
                EnhancedFieldOptions fieldOptions = parseEnhancedFieldOptions(field.getOptions());

                // Resolve final formatting rules (schema + field level)
                FieldFormattingOptions finalFormatting = schemaFormattingService.resolveFieldFormatting(
                        field, schemaRules, fieldOptions.getFormatting()
                );

                // Generate field value with context
                String value = generateFieldValueWithContext(
                        field, fieldOptions, finalFormatting, row, generationContext
                );

                // Store in row and update context for dependent fields
                row.put(field.getName(), value);
                generationContext.put(field.getName(), value);

            } catch (Exception e) {
                log.error("Error generating field '{}' in row {}: {}",
                        field.getName(), rowIndex, e.getMessage(), e);
                row.put(field.getName(), "[ERROR: " + e.getMessage() + "]");
                generationContext.put(field.getName(), "");
            }
        }

        return row;
    }

    /**
     * Generate a single field value with full context and formatting
     */
    private String generateFieldValueWithContext(
            FieldDefinitionDto field,
            EnhancedFieldOptions fieldOptions,
            FieldFormattingOptions finalFormatting,
            Map<String, Object> rowContext,
            Map<String, Object> generationContext) {

        String rawValue;

        // Check if this is a dependent field with an expression
        if (fieldOptions.getDependency() != null &&
                fieldOptions.getDependency().getExpression() != null) {

            // Generate value using expression evaluation
            rawValue = expressionEvaluator.evaluateExpression(
                    fieldOptions.getDependency().getExpression(),
                    rowContext,
                    generationContext,
                    faker
            );

        } else {
            // Generate value using standard data type generator
            DataTypeGenerator generator = getGenerator(
                    fieldOptions.getBaseType() != null ? fieldOptions.getBaseType() : field.getDataType()
            );

            if (generator == null) {
                log.warn("No generator found for type: {}", field.getDataType());
                return "[NO_GENERATOR]";
            }

            rawValue = generator.generate(
                    faker,
                    fieldOptions.getBaseOptions(),
                    rowContext
            );
        }

        // Apply formatting rules
        return fieldFormatter.applyFormatting(rawValue, finalFormatting);
    }

    /**
     * Parse enhanced field options from JSON string
     */
    private EnhancedFieldOptions parseEnhancedFieldOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return new EnhancedFieldOptions();
        }

        try {
            // Try to parse as JSON first (new format)
            return objectMapper.readValue(optionsJson, EnhancedFieldOptions.class);
        } catch (JsonProcessingException e) {
            // Fallback for old string format
            log.debug("Using legacy options format for: {}", optionsJson);
            EnhancedFieldOptions options = new EnhancedFieldOptions();
            options.setBaseOptions(optionsJson);
            return options;
        }
    }

    /**
     * Get the appropriate data type generator
     */
    private DataTypeGenerator getGenerator(String dataType) {
        return dataTypeGenerators.stream()
                .filter(generator -> generator.getKey().equals(dataType))
                .findFirst()
                .orElse(null);
    }

    /**
     * Format the generated data according to the requested output format
     */
    private String formatOutput(
            List<Map<String, Object>> data,
            String format,
            List<FieldDefinitionDto> schema,
            String tableName) {

        if (data.isEmpty()) {
            return "";
        }

        switch (format.toUpperCase()) {
            case "CSV":
                return formatAsCSV(data, schema);
            case "JSON":
                return formatAsJSON(data);
            case "SQL":
                return formatAsSQL(data, schema, tableName);
            case "XML":
                return formatAsXML(data, schema, tableName);
            case "PLAINTEXT":
                return formatAsPlainText(data, schema);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    /**
     * Format data as CSV
     */
    private String formatAsCSV(List<Map<String, Object>> data, List<FieldDefinitionDto> schema) {
        StringBuilder csv = new StringBuilder();

        // Header row
        csv.append(schema.stream()
                        .map(FieldDefinitionDto::getName)
                        .collect(Collectors.joining(",")))
                .append("\n");

        // Data rows
        for (Map<String, Object> row : data) {
            csv.append(schema.stream()
                            .map(field -> {
                                Object value = row.get(field.getName());
                                String stringValue = value != null ? value.toString() : "";
                                // Escape CSV values that contain commas, quotes, or newlines
                                if (stringValue.contains(",") || stringValue.contains("\"") || stringValue.contains("\n")) {
                                    return "\"" + stringValue.replace("\"", "\"\"") + "\"";
                                }
                                return stringValue;
                            })
                            .collect(Collectors.joining(",")))
                    .append("\n");
        }

        return csv.toString();
    }

    /**
     * Format data as JSON
     */
    private String formatAsJSON(List<Map<String, Object>> data) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error formatting data as JSON", e);
            throw new RuntimeException("Failed to format data as JSON", e);
        }
    }

    /**
     * Format data as SQL INSERT statements
     */
    private String formatAsSQL(List<Map<String, Object>> data, List<FieldDefinitionDto> schema, String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            tableName = "generated_data";
        }

        StringBuilder sql = new StringBuilder();

        // Create table statement (optional, commented out)
        sql.append("-- CREATE TABLE ").append(tableName).append(" (\n");
        for (int i = 0; i < schema.size(); i++) {
            FieldDefinitionDto field = schema.get(i);
            sql.append("--   ").append(field.getName()).append(" VARCHAR(255)");
            if (i < schema.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }
        sql.append("-- );\n\n");

        // INSERT statements
        String columns = schema.stream()
                .map(FieldDefinitionDto::getName)
                .collect(Collectors.joining(", "));

        for (Map<String, Object> row : data) {
            sql.append("INSERT INTO ").append(tableName).append(" (").append(columns).append(") VALUES (");

            sql.append(schema.stream()
                    .map(field -> {
                        Object value = row.get(field.getName());
                        if (value == null) {
                            return "NULL";
                        }
                        // Escape single quotes and wrap in quotes
                        String stringValue = value.toString().replace("'", "''");
                        return "'" + stringValue + "'";
                    })
                    .collect(Collectors.joining(", ")));

            sql.append(");\n");
        }

        return sql.toString();
    }

    /**
     * Format data as XML
     */
    private String formatAsXML(List<Map<String, Object>> data, List<FieldDefinitionDto> schema, String tableName) {
        StringBuilder xml = new StringBuilder();
        String rootElement = tableName != null ? tableName : "data";

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<").append(rootElement).append(">\n");

        for (Map<String, Object> row : data) {
            xml.append("  <record>\n");
            for (FieldDefinitionDto field : schema) {
                Object value = row.get(field.getName());
                String stringValue = value != null ? escapeXml(value.toString()) : "";
                xml.append("    <").append(field.getName()).append(">")
                        .append(stringValue)
                        .append("</").append(field.getName()).append(">\n");
            }
            xml.append("  </record>\n");
        }

        xml.append("</").append(rootElement).append(">\n");
        return xml.toString();
    }

    /**
     * Format data as plain text table
     */
    private String formatAsPlainText(List<Map<String, Object>> data, List<FieldDefinitionDto> schema) {
        if (data.isEmpty()) {
            return "No data generated.";
        }

        // Calculate column widths
        Map<String, Integer> columnWidths = new HashMap<>();

        // Initialize with header lengths
        for (FieldDefinitionDto field : schema) {
            columnWidths.put(field.getName(), field.getName().length());
        }

        // Update with data lengths
        for (Map<String, Object> row : data) {
            for (FieldDefinitionDto field : schema) {
                Object value = row.get(field.getName());
                String stringValue = value != null ? value.toString() : "";
                columnWidths.put(field.getName(),
                        Math.max(columnWidths.get(field.getName()), stringValue.length()));
            }
        }

        StringBuilder text = new StringBuilder();

        // Header
        for (FieldDefinitionDto field : schema) {
            text.append(String.format("%-" + (columnWidths.get(field.getName()) + 2) + "s", field.getName()));
        }
        text.append("\n");

        // Separator line
        for (FieldDefinitionDto field : schema) {
            text.append("-".repeat(columnWidths.get(field.getName()) + 2));
        }
        text.append("\n");

        // Data rows
        for (Map<String, Object> row : data) {
            for (FieldDefinitionDto field : schema) {
                Object value = row.get(field.getName());
                String stringValue = value != null ? value.toString() : "";
                text.append(String.format("%-" + (columnWidths.get(field.getName()) + 2) + "s", stringValue));
            }
            text.append("\n");
        }

        return text.toString();
    }

    /**
     * Escape XML special characters
     */
    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Get available data types for frontend
     */
    public List<Map<String, String>> getAvailableDataTypes() {
        return dataTypeGenerators.stream()
                .map(generator -> {
                    Map<String, String> typeInfo = new HashMap<>();
                    typeInfo.put("key", generator.getKey());
                    typeInfo.put("name", generator.getName());
                    typeInfo.put("category", generator.getCategory());
                    return typeInfo;
                })
                .sorted((a, b) -> {
                    // Sort by category first, then by name
                    int categoryCompare = a.get("category").compareTo(b.get("category"));
                    if (categoryCompare != 0) {
                        return categoryCompare;
                    }
                    return a.get("name").compareTo(b.get("name"));
                })
                .collect(Collectors.toList());
    }

    /**
     * Validate generation request
     */
    public void validateGenerationRequest(GenerationRequest request) {
        if (request.getSchema() == null || request.getSchema().isEmpty()) {
            throw new IllegalArgumentException("Schema cannot be empty");
        }

        if (request.getRowCount() <= 0) {
            throw new IllegalArgumentException("Row count must be greater than 0");
        }

        if (request.getRowCount() > 100000) {
            throw new IllegalArgumentException("Row count cannot exceed 100,000");
        }

        if (request.getFormat() == null || request.getFormat().trim().isEmpty()) {
            throw new IllegalArgumentException("Format cannot be empty");
        }

        if ("SQL".equalsIgnoreCase(request.getFormat()) &&
                (request.getTableName() == null || request.getTableName().trim().isEmpty())) {
            throw new IllegalArgumentException("Table name is required for SQL format");
        }
    }
}