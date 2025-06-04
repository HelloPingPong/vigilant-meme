package com.jpmorgan.reactdemo.service;


import com.jpmorgan.reactdemo.dto.*;
import com.jpmorgan.reactdemo.formatting.FieldFormatter;
import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.javafaker.Faker;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DataGenerationService {

    static final Logger log = LoggerFactory.getLogger(DataGenerationService.class);
    private final List<DataTypeGenerator> generators; // Injected by Spring
    private final ObjectMapper objectMapper; // For JSON
    private final CsvMapper csvMapper; // For CSV
    private final XmlMapper xmlMapper; // For XML

    @Autowired
    public DataGenerationService(List<DataTypeGenerator> generators, ObjectMapper objectMapper,
            @Qualifier("csvMapper") CsvMapper csvMapper,
            @Qualifier("xmlMapper") XmlMapper xmlMapper) {
        this.generators = generators;
        this.objectMapper = objectMapper;
        this.csvMapper = csvMapper;
        this.xmlMapper = xmlMapper;
    }

    final Faker faker = new Faker(); // Create one instance

    Map<String, DataTypeGenerator> generatorMap;

    // Methods to handle JSON, CSV, and XML separately
    public String handleJson(Object data) throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper(); // Use default ObjectMapper for JSON
        return jsonMapper.writeValueAsString(data);
    }

    public String handleCsv(Object data) throws JsonProcessingException {
        // Use csvMapper for CSV-specific logic
        return csvMapper.writeValueAsString(data);
    }

    public String handleXml(Object data) throws JsonProcessingException {
        // Use xmlMapper for XML-specific logic
        return xmlMapper.writeValueAsString(data);
    }

    @PostConstruct
    public void init() {
        // Create a map for easy lookup
        generatorMap = generators.stream()
                .collect(Collectors.toMap(DataTypeGenerator::getKey, gen -> gen));
        log.info("Initialized {} data type generators.", generatorMap.size());
    }

    public List<DataTypeInfo> getSupportedDataTypes() {
        return generators.stream()
                .map(gen -> new DataTypeInfo(gen.getKey(), gen.getName(), gen.getCategory()))
                .sorted(Comparator.comparing(DataTypeInfo::getCategory).thenComparing(DataTypeInfo::getName))
                .collect(Collectors.toList());
    }

    public String generateData(GenerationRequest request) throws Exception {
        List<Map<String, Object>> data = generateRawData(request.getSchema(), request.getRowCount());
        return formatData(data, request.getFormat(), request.getSchema(), request.getTableName());
    }

    public List<Map<String, Object>> generateRawData(List<FieldDefinitionDto> schema, int rowCount) {
        return IntStream.range(0, rowCount)
                .mapToObj(i -> generateRow(schema))
                .collect(Collectors.toList());
    }

    private Map<String, Object> generateRow(List<FieldDefinitionDto> schema) {
        Map<String, Object> row = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order
        for (FieldDefinitionDto field : schema) {
            DataTypeGenerator generator = generatorMap.get(field.getDataType());
            if (generator != null) {
                try {
                    row.put(field.getName(), generator.generate(faker, field.getOptions()));
                } catch (Exception e) {
                    log.error("Error generating data for field '{}' with type '{}': {}", field.getName(), field.getDataType(), e.getMessage());
                    row.put(field.getName(), "[ERROR]"); // Indicate error in output
                }
            } else {
                log.warn("No generator found for data type: {}", field.getDataType());
                row.put(field.getName(), "[UNKNOWN TYPE]");
            }
        }
        return row;
    }

    String formatData(List<Map<String, Object>> data, String format, List<FieldDefinitionDto> schema, String tableName) throws Exception {
        return switch (format.toUpperCase()) {
            case "JSON" -> formatJson(data);
            case "CSV" -> formatCsv(data, schema);
            case "XML" -> formatXml(data);
            case "SQL" -> formatSql(data, schema, tableName);
            case "PLAINTEXT" -> formatPlainText(data, schema);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    private String formatJson(List<Map<String, Object>> data) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }

    private String formatCsv(List<Map<String, Object>> data, List<FieldDefinitionDto> schema) throws JsonProcessingException {
        if (data.isEmpty()) return "";

        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        List<String> headers = schema.stream().map(FieldDefinitionDto::getName).toList();
        for (String header : headers) {
            schemaBuilder.addColumn(header);
        }
        CsvSchema csvSchema = schemaBuilder.build().withHeader();

        return csvMapper.writer(csvSchema).writeValueAsString(data);
    }

    private String formatXml(List<Map<String, Object>> data) throws JsonProcessingException {
        // Basic XML structure, might need refinement based on desired output
        return xmlMapper.writerWithDefaultPrettyPrinter().withRootName("rows").writeValueAsString(data.stream().map(row -> Map.of("row", row)).toList());
        // Alternatively structure as <rows><row><field>value</field></row>...</rows> requires more custom serialization
    }

    private String formatSql(List<Map<String, Object>> data, List<FieldDefinitionDto> schema, String tableName) {
        if (data.isEmpty()) return "-- No data to generate SQL for";
        if (!StringUtils.hasText(tableName)) {
            return "-- Error: Table name is required for SQL format.";
        }

        String columns = schema.stream()
                .map(f -> escapeSqlIdentifier(f.getName()))
                .collect(Collectors.joining(", "));

        StringBuilder sql = new StringBuilder();
        String insertPrefix = "INSERT INTO " + escapeSqlIdentifier(tableName) + " (" + columns + ") VALUES ";
        sql.append(insertPrefix);

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            String values = schema.stream()
                    .map(f -> formatSqlValue(row.get(f.getName())))
                    .collect(Collectors.joining(", "));
            sql.append("(").append(values).append(")");
            if (i < data.size() - 1) {
                sql.append(",\n").append(insertPrefix.replaceAll(".", " ")); // Align values for readability
            } else {
                sql.append(";\n");
            }
        }
        return sql.toString();
    }

    private String formatPlainText(List<Map<String, Object>> data, List<FieldDefinitionDto> schema) {
        if (data.isEmpty()) return "";

        // Simple tab-separated values
        StringBuilder sb = new StringBuilder();
        // Header
        sb.append(schema.stream().map(FieldDefinitionDto::getName).collect(Collectors.joining("\t"))).append("\n");
        // Data
        data.forEach(row -> {
            sb.append(schema.stream()
                    .map(f -> String.valueOf(row.getOrDefault(f.getName(), "")))
                    .collect(Collectors.joining("\t"))).append("\n");
        });
        return sb.toString();
    }

    // --- SQL Helper Methods ---
    private String escapeSqlIdentifier(String identifier) {
        // Basic escaping for standard SQL, adjust if needed for specific DBs
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString(); // Numbers don't need quotes
        }
        // Treat everything else as string, escape single quotes
        return "'" + value.toString().replace("'", "''") + "'";
    }

}
