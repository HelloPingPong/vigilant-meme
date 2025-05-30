package com.jpmorgan.reactdemo.expression;

import lombok.Data;
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
public class ExpressionParser {

    private static final Pattern FIELD_REFERENCE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\(([^)]*)\\)");
    private static final Pattern NESTED_FUNCTION_PATTERN = Pattern.compile("(\\w+)\\(([^()]*(?:\\([^()]*\\)[^()]*)*)\\)");

    // Known function names for validation
    private static final Set<String> KNOWN_FUNCTIONS = Set.of(
            "randomnumber", "substring", "uppercase", "lowercase", "replace",
            "concat", "length", "padleft", "padright", "formatdate",
            "randomchoice", "conditional"
    );

    /**
     * Parse an expression and return detailed information about its structure
     */
    public ParsedExpression parseExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new ParsedExpression(expression, true, Collections.emptyList(),
                    Collections.emptySet(), Collections.emptyList(),
                    Collections.emptyList());
        }

        log.debug("Parsing expression: {}", expression);

        List<String> errors = new ArrayList<>();
        Set<String> fieldReferences = new HashSet<>();
        List<FunctionCall> functionCalls = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Extract field references
            fieldReferences.addAll(extractFieldReferences(expression));

            // Extract and validate function calls
            functionCalls.addAll(extractFunctionCalls(expression, errors, warnings));

            // Validate overall expression syntax
            validateExpressionSyntax(expression, errors);

            // Check for potential issues
            checkForPotentialIssues(expression, fieldReferences, functionCalls, warnings);

        } catch (Exception e) {
            log.error("Error parsing expression '{}': {}", expression, e.getMessage(), e);
            errors.add("Parse error: " + e.getMessage());
        }

        boolean isValid = errors.isEmpty();

        ParsedExpression result = new ParsedExpression(
                expression, isValid, errors, fieldReferences, functionCalls, warnings
        );

        log.debug("Parsed expression - Valid: {}, Field refs: {}, Functions: {}, Errors: {}",
                isValid, fieldReferences.size(), functionCalls.size(), errors.size());

        return result;
    }

    /**
     * Extract all field references from an expression
     */
    private Set<String> extractFieldReferences(String expression) {
        Set<String> references = new HashSet<>();
        Matcher matcher = FIELD_REFERENCE_PATTERN.matcher(expression);

        while (matcher.find()) {
            String fieldRef = matcher.group(1).trim();
            if (!fieldRef.isEmpty()) {
                // Handle function calls within field references
                if (fieldRef.contains("(")) {
                    // This might be a function that references a field
                    String cleanRef = extractFieldFromFunctionReference(fieldRef);
                    if (cleanRef != null) {
                        references.add(cleanRef);
                    }
                } else {
                    references.add(fieldRef);
                }
            }
        }

        return references;
    }

    /**
     * Extract field name from a function reference within ${...}
     */
    private String extractFieldFromFunctionReference(String functionRef) {
        // Handle cases like ${substring(fieldName, 0, 3)}
        Matcher matcher = FUNCTION_PATTERN.matcher(functionRef);
        if (matcher.find()) {
            String functionName = matcher.group(1).toLowerCase();
            String params = matcher.group(2);

            // For functions that take field names as first parameter
            if (isFunctionThatReferencesField(functionName)) {
                String[] paramArray = splitParameters(params);
                if (paramArray.length > 0) {
                    return paramArray[0].trim();
                }
            }
        }
        return null;
    }

    /**
     * Check if a function typically references a field as its first parameter
     */
    private boolean isFunctionThatReferencesField(String functionName) {
        return Arrays.asList("substring", "uppercase", "lowercase", "length",
                "padleft", "padright").contains(functionName.toLowerCase());
    }

    /**
     * Extract all function calls from an expression
     */
    private List<FunctionCall> extractFunctionCalls(String expression, List<String> errors, List<String> warnings) {
        List<FunctionCall> functionCalls = new ArrayList<>();

        // Use nested function pattern to handle complex expressions
        Matcher matcher = NESTED_FUNCTION_PATTERN.matcher(expression);

        while (matcher.find()) {
            String functionName = matcher.group(1);
            String parameters = matcher.group(2);

            try {
                FunctionCall functionCall = parseFunctionCall(functionName, parameters, errors, warnings);
                functionCalls.add(functionCall);
            } catch (Exception e) {
                errors.add("Error parsing function '" + functionName + "': " + e.getMessage());
            }
        }

        return functionCalls;
    }

    /**
     * Parse a single function call
     */
    private FunctionCall parseFunctionCall(String functionName, String parameters,
            List<String> errors, List<String> warnings) {

        // Validate function name
        if (!KNOWN_FUNCTIONS.contains(functionName.toLowerCase())) {
            warnings.add("Unknown function: " + functionName);
        }

        // Parse parameters
        List<String> paramList = new ArrayList<>();
        if (parameters != null && !parameters.trim().isEmpty()) {
            try {
                paramList = Arrays.asList(splitParameters(parameters));
            } catch (Exception e) {
                errors.add("Invalid parameters for function '" + functionName + "': " + e.getMessage());
            }
        }

        // Validate parameter count for known functions
        validateFunctionParameters(functionName, paramList, errors, warnings);

        return new FunctionCall(functionName, paramList,
                extractFieldReferencesFromParameters(paramList));
    }

    /**
     * Split parameters handling nested functions and quoted strings
     */
    private String[] splitParameters(String parameters) {
        if (parameters == null || parameters.trim().isEmpty()) {
            return new String[0];
        }

        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';
        int depth = 0;

        for (int i = 0; i < parameters.length(); i++) {
            char c = parameters.charAt(i);

            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
                current.append(c);
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                current.append(c);
            } else if (!inQuotes && c == '(') {
                depth++;
                current.append(c);
            } else if (!inQuotes && c == ')') {
                depth--;
                current.append(c);
            } else if (!inQuotes && c == ',' && depth == 0) {
                params.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            params.add(current.toString().trim());
        }

        return params.toArray(new String[0]);
    }

    /**
     * Extract field references from function parameters
     */
    private Set<String> extractFieldReferencesFromParameters(List<String> parameters) {
        Set<String> references = new HashSet<>();

        for (String param : parameters) {
            Matcher matcher = FIELD_REFERENCE_PATTERN.matcher(param);
            while (matcher.find()) {
                String fieldRef = matcher.group(1).trim();
                if (!fieldRef.isEmpty()) {
                    references.add(fieldRef);
                }
            }
        }

        return references;
    }

    /**
     * Validate function parameters for known functions
     */
    private void validateFunctionParameters(String functionName, List<String> parameters,
            List<String> errors, List<String> warnings) {
        String funcName = functionName.toLowerCase();
        int paramCount = parameters.size();

        switch (funcName) {
            case "randomnumber":
                if (paramCount != 2) {
                    errors.add("randomNumber requires exactly 2 parameters (min, max)");
                } else {
                    validateNumericParameters(parameters, errors, "randomNumber");
                }
                break;

            case "substring":
                if (paramCount < 2 || paramCount > 3) {
                    errors.add("substring requires 2 or 3 parameters (value, start[, end])");
                } else {
                    validateNumericParameter(parameters.get(1), errors, "substring start index");
                    if (paramCount == 3) {
                        validateNumericParameter(parameters.get(2), errors, "substring end index");
                    }
                }
                break;

            case "uppercase":
            case "lowercase":
            case "length":
                if (paramCount != 1) {
                    errors.add(functionName + " requires exactly 1 parameter");
                }
                break;

            case "replace":
                if (paramCount != 3) {
                    errors.add("replace requires exactly 3 parameters (value, search, replacement)");
                }
                break;

            case "concat":
                if (paramCount < 2) {
                    errors.add("concat requires at least 2 parameters");
                }
                break;

            case "padleft":
            case "padright":
                if (paramCount < 2 || paramCount > 3) {
                    errors.add(functionName + " requires 2 or 3 parameters (value, length[, padChar])");
                } else {
                    validateNumericParameter(parameters.get(1), errors, functionName + " length");
                }
                break;

            case "randomchoice":
                if (paramCount < 2) {
                    errors.add("randomChoice requires at least 2 options");
                }
                break;

            case "conditional":
                if (paramCount != 3) {
                    errors.add("conditional requires exactly 3 parameters (condition, valueIfTrue, valueIfFalse)");
                }
                break;

            case "formatdate":
                if (paramCount < 1 || paramCount > 2) {
                    errors.add("formatDate requires 1 or 2 parameters ([value,] format)");
                }
                break;
        }
    }

    /**
     * Validate that parameters are numeric (for functions that require numbers)
     */
    private void validateNumericParameters(List<String> parameters, List<String> errors, String functionName) {
        for (int i = 0; i < parameters.size(); i++) {
            validateNumericParameter(parameters.get(i), errors, functionName + " parameter " + (i + 1));
        }
    }

    /**
     * Validate a single numeric parameter
     */
    private void validateNumericParameter(String parameter, List<String> errors, String context) {
        if (parameter != null && !parameter.trim().isEmpty() && !parameter.contains("${")) {
            try {
                Integer.parseInt(parameter.trim().replace("'", "").replace("\"", ""));
            } catch (NumberFormatException e) {
                // Only add error if it's clearly meant to be a number (not a field reference)
                if (!parameter.contains("${") && !parameter.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    errors.add("Invalid numeric value for " + context + ": " + parameter);
                }
            }
        }
    }

    /**
     * Validate overall expression syntax
     */
    private void validateExpressionSyntax(String expression, List<String> errors) {
        // Check for balanced braces
        if (!hasBalancedBraces(expression)) {
            errors.add("Unbalanced braces in expression");
        }

        // Check for balanced parentheses in functions
        if (!hasBalancedParentheses(expression)) {
            errors.add("Unbalanced parentheses in expression");
        }

        // Check for empty field references
        if (expression.contains("${}")) {
            errors.add("Empty field reference found: ${}");
        }

        // Check for malformed field references
        Pattern malformedPattern = Pattern.compile("\\$[^{]|\\$\\{[^}]*$");
        if (malformedPattern.matcher(expression).find()) {
            errors.add("Malformed field reference (missing braces or incomplete)");
        }
    }

    /**
     * Check if braces are balanced
     */
    private boolean hasBalancedBraces(String expression) {
        return isBalanced(expression, '{', '}');
    }

    /**
     * Check if parentheses are balanced
     */
    private boolean hasBalancedParentheses(String expression) {
        return isBalanced(expression, '(', ')');
    }

    /**
     * Generic method to check if characters are balanced
     */
    private boolean isBalanced(String expression, char open, char close) {
        int count = 0;
        boolean inQuotes = false;
        char quoteChar = '"';

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
            } else if (!inQuotes) {
                if (c == open) {
                    count++;
                } else if (c == close) {
                    count--;
                    if (count < 0) {
                        return false;
                    }
                }
            }
        }

        return count == 0;
    }

    /**
     * Check for potential issues and generate warnings
     */
    private void checkForPotentialIssues(String expression, Set<String> fieldReferences,
            List<FunctionCall> functionCalls, List<String> warnings) {

        // Check for very long expressions
        if (expression.length() > 500) {
            warnings.add("Expression is very long (" + expression.length() + " characters) - consider simplifying");
        }

        // Check for deeply nested function calls
        int maxNesting = calculateMaxNestingLevel(expression);
        if (maxNesting > 5) {
            warnings.add("Deep function nesting detected (level " + maxNesting + ") - may impact performance");
        }

        // Check for potential infinite recursion in field references
        for (String fieldRef : fieldReferences) {
            if (fieldRef.equals("_self") || fieldRef.equals("self")) {
                warnings.add("Self-reference detected - this may cause issues");
            }
        }

        // Check for common typos in field references
        for (String fieldRef : fieldReferences) {
            if (fieldRef.startsWith("_") && !fieldRef.equals("_rowIndex") && !fieldRef.equals("_timestamp")) {
                warnings.add("Field reference '" + fieldRef + "' starts with underscore - ensure this is intended");
            }
        }

        // Check for potentially expensive operations
        long randomFunctionCount = functionCalls.stream()
                .filter(fc -> fc.getFunctionName().toLowerCase().contains("random"))
                .count();
        if (randomFunctionCount > 3) {
            warnings.add("Multiple random functions detected - this may impact consistency");
        }
    }

    /**
     * Calculate maximum nesting level of function calls
     */
    private int calculateMaxNestingLevel(String expression) {
        int maxLevel = 0;
        int currentLevel = 0;
        boolean inQuotes = false;
        char quoteChar = '"';

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
            } else if (!inQuotes && c == '(') {
                currentLevel++;
                maxLevel = Math.max(maxLevel, currentLevel);
            } else if (!inQuotes && c == ')') {
                currentLevel--;
            }
        }

        return maxLevel;
    }

    /**
     * Get all unique field references from an expression
     */
    public Set<String> getFieldReferences(String expression) {
        return parseExpression(expression).getFieldReferences();
    }

    /**
     * Check if an expression is valid
     */
    public boolean isValidExpression(String expression) {
        return parseExpression(expression).isValid();
    }

    /**
     * Get validation errors for an expression
     */
    public List<String> getValidationErrors(String expression) {
        return parseExpression(expression).getErrors();
    }

    /**
     * Normalize an expression (remove extra whitespace, standardize formatting)
     */
    public String normalizeExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return expression;
        }

        // Remove extra whitespace but preserve spaces in strings
        StringBuilder normalized = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';
        boolean lastWasSpace = false;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
                normalized.append(c);
                lastWasSpace = false;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                normalized.append(c);
                lastWasSpace = false;
            } else if (inQuotes) {
                normalized.append(c);
                lastWasSpace = false;
            } else if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    normalized.append(' ');
                    lastWasSpace = true;
                }
            } else {
                normalized.append(c);
                lastWasSpace = false;
            }
        }

        return normalized.toString().trim();
    }

    /**
     * Data class representing a parsed expression
     */
    @Data
    public static class ParsedExpression {
        private final String originalExpression;
        private final boolean valid;
        private final List<String> errors;
        private final Set<String> fieldReferences;
        private final List<FunctionCall> functionCalls;
        private final List<String> warnings;

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean hasFieldReferences() {
            return !fieldReferences.isEmpty();
        }

        public boolean hasFunctionCalls() {
            return !functionCalls.isEmpty();
        }
    }

    /**
     * Data class representing a function call within an expression
     */
    @Data
    public static class FunctionCall {
        private final String functionName;
        private final List<String> parameters;
        private final Set<String> referencedFields;

        public boolean hasFieldReferences() {
            return !referencedFields.isEmpty();
        }

        public int getParameterCount() {
            return parameters.size();
        }
    }
}