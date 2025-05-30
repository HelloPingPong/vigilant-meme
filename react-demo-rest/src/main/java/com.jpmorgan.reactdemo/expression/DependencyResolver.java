package com.jpmorgan.reactdemo.expression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpmorgan.reactdemo.dto.EnhancedFieldOptions;
import com.jpmorgan.reactdemo.dto.FieldDefinitionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DependencyResolver {

    private final ObjectMapper objectMapper;
    private static final Pattern FIELD_REFERENCE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Resolve the dependency order of fields using topological sort
     */
    public List<FieldDefinitionDto> resolveDependencyOrder(List<FieldDefinitionDto> schema) {
        log.debug("Resolving dependency order for {} fields", schema.size());

        // Build dependency graph
        Map<String, Set<String>> dependencies = buildDependencyGraph(schema);

        // Perform topological sort
        List<FieldDefinitionDto> sortedFields = topologicalSort(schema, dependencies);

        log.debug("Dependency order resolved. Field order: {}",
                sortedFields.stream().map(FieldDefinitionDto::getName).collect(Collectors.toList()));

        return sortedFields;
    }

    /**
     * Build a dependency graph from the schema
     */
    private Map<String, Set<String>> buildDependencyGraph(List<FieldDefinitionDto> schema) {
        Map<String, Set<String>> dependencies = new HashMap<>();

        // Initialize empty dependency sets for all fields
        for (FieldDefinitionDto field : schema) {
            dependencies.put(field.getName(), new HashSet<>());
        }

        // Analyze each field for dependencies
        for (FieldDefinitionDto field : schema) {
            Set<String> fieldDependencies = extractFieldDependencies(field);
            dependencies.put(field.getName(), fieldDependencies);
        }

        return dependencies;
    }

    /**
     * Extract field dependencies from a field definition
     */
    private Set<String> extractFieldDependencies(FieldDefinitionDto field) {
        Set<String> dependencies = new HashSet<>();

        try {
            EnhancedFieldOptions options = parseEnhancedFieldOptions(field.getOptions());

            // Extract dependencies from expression
            if (options.getDependency() != null && options.getDependency().getExpression() != null) {
                Set<String> expressionDeps = extractDependenciesFromExpression(options.getDependency().getExpression());
                dependencies.addAll(expressionDeps);
            }

            // Extract explicit dependencies
            if (options.getDependency() != null && options.getDependency().getDependsOn() != null) {
                dependencies.addAll(options.getDependency().getDependsOn());
            }

        } catch (Exception e) {
            log.warn("Error extracting dependencies for field '{}': {}", field.getName(), e.getMessage());
        }

        return dependencies;
    }

    /**
     * Extract field references from an expression string
     */
    private Set<String> extractDependenciesFromExpression(String expression) {
        Set<String> dependencies = new HashSet<>();

        if (expression == null || expression.trim().isEmpty()) {
            return dependencies;
        }

        Matcher matcher = FIELD_REFERENCE_PATTERN.matcher(expression);
        while (matcher.find()) {
            String fieldReference = matcher.group(1);

            // Handle nested function calls and complex references
            String cleanFieldName = extractCleanFieldName(fieldReference);
            if (cleanFieldName != null && !cleanFieldName.trim().isEmpty()) {
                dependencies.add(cleanFieldName);
            }
        }

        return dependencies;
    }

    /**
     * Extract clean field name from a potentially complex field reference
     */
    private String extractCleanFieldName(String fieldReference) {
        // Handle function calls like substring(firstName,0,3)
        if (fieldReference.contains("(")) {
            // Extract the first parameter which should be the field name
            int openParen = fieldReference.indexOf('(');
            String functionName = fieldReference.substring(0, openParen);
            String params = fieldReference.substring(openParen + 1);

            // For functions that take field names as first parameter
            if (isFunctionThatTakesFieldName(functionName)) {
                int firstComma = params.indexOf(',');
                if (firstComma > 0) {
                    return params.substring(0, firstComma).trim();
                } else {
                    // Single parameter
                    int closeParen = params.lastIndexOf(')');
                    if (closeParen > 0) {
                        return params.substring(0, closeParen).trim();
                    }
                }
            }
            return null; // Function doesn't reference other fields
        }

        // Simple field reference
        return fieldReference.trim();
    }

    /**
     * Check if a function takes a field name as its first parameter
     */
    private boolean isFunctionThatTakesFieldName(String functionName) {
        return Arrays.asList("substring", "uppercase", "lowercase", "replace", "length").contains(functionName.toLowerCase());
    }

    /**
     * Perform topological sort on the dependency graph
     */
    private List<FieldDefinitionDto> topologicalSort(
            List<FieldDefinitionDto> schema,
            Map<String, Set<String>> dependencies) {

        List<FieldDefinitionDto> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        Map<String, FieldDefinitionDto> fieldMap = schema.stream()
                .collect(Collectors.toMap(FieldDefinitionDto::getName, f -> f));

        for (FieldDefinitionDto field : schema) {
            if (!visited.contains(field.getName())) {
                if (topologicalSortVisit(field.getName(), dependencies, fieldMap, visited, visiting, result)) {
                    // Circular dependency detected - log warning and continue with original order
                    log.warn("Circular dependency detected in schema. Using original field order.");
                    return schema;
                }
            }
        }

        return result;
    }

    /**
     * Recursive helper for topological sort
     */
    private boolean topologicalSortVisit(
            String fieldName,
            Map<String, Set<String>> dependencies,
            Map<String, FieldDefinitionDto> fieldMap,
            Set<String> visited,
            Set<String> visiting,
            List<FieldDefinitionDto> result) {

        if (visiting.contains(fieldName)) {
            // Circular dependency detected
            return true;
        }

        if (visited.contains(fieldName)) {
            return false;
        }

        visiting.add(fieldName);

        // Visit all dependencies first
        Set<String> fieldDependencies = dependencies.getOrDefault(fieldName, Collections.emptySet());
        for (String dependency : fieldDependencies) {
            // Only process dependencies that exist in our schema
            if (fieldMap.containsKey(dependency)) {
                if (topologicalSortVisit(dependency, dependencies, fieldMap, visited, visiting, result)) {
                    return true; // Circular dependency
                }
            } else {
                log.warn("Field '{}' depends on '{}' which doesn't exist in schema", fieldName, dependency);
            }
        }

        visiting.remove(fieldName);
        visited.add(fieldName);

        // Add field to result if it exists in our schema
        if (fieldMap.containsKey(fieldName)) {
            result.add(fieldMap.get(fieldName));
        }

        return false;
    }

    /**
     * Parse enhanced field options with fallback for legacy format
     */
    private EnhancedFieldOptions parseEnhancedFieldOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return new EnhancedFieldOptions();
        }

        try {
            return objectMapper.readValue(optionsJson, EnhancedFieldOptions.class);
        } catch (JsonProcessingException e) {
            // Legacy format - no dependencies
            return new EnhancedFieldOptions();
        }
    }

    /**
     * Validate that there are no circular dependencies in the schema
     */
    public void validateNoCycles(List<FieldDefinitionDto> schema) throws IllegalArgumentException {
        Map<String, Set<String>> dependencies = buildDependencyGraph(schema);

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (FieldDefinitionDto field : schema) {
            if (!visited.contains(field.getName())) {
                if (hasCycle(field.getName(), dependencies, visited, visiting)) {
                    throw new IllegalArgumentException(
                            "Circular dependency detected involving field: " + field.getName());
                }
            }
        }
    }

    /**
     * Check for cycles using DFS
     */
    private boolean hasCycle(
            String fieldName,
            Map<String, Set<String>> dependencies,
            Set<String> visited,
            Set<String> visiting) {

        if (visiting.contains(fieldName)) {
            return true; // Cycle detected
        }

        if (visited.contains(fieldName)) {
            return false;
        }

        visiting.add(fieldName);

        Set<String> fieldDependencies = dependencies.getOrDefault(fieldName, Collections.emptySet());
        for (String dependency : fieldDependencies) {
            if (hasCycle(dependency, dependencies, visited, visiting)) {
                return true;
            }
        }

        visiting.remove(fieldName);
        visited.add(fieldName);

        return false;
    }

    /**
     * Get dependency information for debugging
     */
    public Map<String, Set<String>> getDependencyInfo(List<FieldDefinitionDto> schema) {
        return buildDependencyGraph(schema);
    }

    /**
     * Create a graphical representation of dependencies for debugging
     */
    public String createDependencyGraph(List<FieldDefinitionDto> schema) {
        Map<String, Set<String>> dependencies = buildDependencyGraph(schema);
        StringBuilder graph = new StringBuilder();

        graph.append("Field Dependencies:\n");
        graph.append("==================\n");

        for (FieldDefinitionDto field : schema) {
            String fieldName = field.getName();
            Set<String> deps = dependencies.get(fieldName);

            graph.append(fieldName);
            if (deps.isEmpty()) {
                graph.append(" -> [no dependencies]");
            } else {
                graph.append(" -> ").append(deps.stream()
                        .sorted()
                        .collect(Collectors.joining(", ")));
            }
            graph.append("\n");
        }

        return graph.toString();
    }

    /**
     * Get fields that have no dependencies (can be generated first)
     */
    public List<String> getIndependentFields(List<FieldDefinitionDto> schema) {
        Map<String, Set<String>> dependencies = buildDependencyGraph(schema);

        return schema.stream()
                .map(FieldDefinitionDto::getName)
                .filter(fieldName -> dependencies.get(fieldName).isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get fields that depend on a specific field
     */
    public List<String> getFieldsDependingOn(List<FieldDefinitionDto> schema, String targetField) {
        Map<String, Set<String>> dependencies = buildDependencyGraph(schema);

        return dependencies.entrySet().stream()
                .filter(entry -> entry.getValue().contains(targetField))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Check if field A depends on field B (directly or indirectly)
     */
    public boolean fieldDependsOn(List<FieldDefinitionDto> schema, String fieldA, String fieldB) {
        Map<String, Set<String>> dependencies = buildDependencyGraph(schema);
        Set<String> visited = new HashSet<>();

        return checkDependencyRecursive(fieldA, fieldB, dependencies, visited);
    }

    private boolean checkDependencyRecursive(
            String current,
            String target,
            Map<String, Set<String>> dependencies,
            Set<String> visited) {

        if (visited.contains(current)) {
            return false; // Avoid infinite loops
        }

        visited.add(current);
        Set<String> currentDeps = dependencies.getOrDefault(current, Collections.emptySet());

        // Direct dependency
        if (currentDeps.contains(target)) {
            return true;
        }

        // Indirect dependency
        for (String dep : currentDeps) {
            if (checkDependencyRecursive(dep, target, dependencies, visited)) {
                return true;
            }
        }

        return false;
    }
}