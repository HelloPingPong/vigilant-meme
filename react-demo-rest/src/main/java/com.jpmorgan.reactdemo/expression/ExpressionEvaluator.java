package com.jpmorgan.reactdemo.expression;

import com.github.javafaker.Faker;
import com.jpmorgan.reactdemo.expression.functions.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ExpressionEvaluator {

    private static final Pattern FIELD_REFERENCE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\(([^)]*)\\)");

    private Map<String, ExpressionFunction> functions;

    @PostConstruct
    public void initializeFunctions() {
        functions = new HashMap<>();

        // Register built-in functions
        registerFunction(new RandomNumberFunction());
        registerFunction(new SubstringFunction());
        registerFunction(new UpperCaseFunction());
        registerFunction(new LowerCaseFunction());
        registerFunction(new ReplaceFunction());
        registerFunction(new ConcatFunction());
        registerFunction(new LengthFunction());
        registerFunction(new PadLeftFunction());
        registerFunction(new PadRightFunction());
        registerFunction(new FormatDateFunction());
        registerFunction(new RandomChoiceFunction());
        registerFunction(new ConditionalFunction());

        log.info("Initialized {} expression functions", functions.size());
    }

    /**
     * Register a new expression function
     */
    public void registerFunction(ExpressionFunction function) {
        functions.put(function.getName().toLowerCase(), function);
        log.debug("Registered expression function: {}", function.getName());
    }

    /**
     * Evaluate an expression with field references and functions
     */
    public String evaluateExpression(
            String expression,
            Map<String, Object> rowContext,
            Map<String, Object> generationContext,
            Faker faker) {

        if (expression == null || expression.trim().isEmpty()) {
            return "";
        }

        log.debug("Evaluating expression: {}", expression);

        try {
            String result = expression;

            // First pass: Replace field references
            result = replaceFieldReferences(result, rowContext, generationContext);

            // Second pass: Evaluate functions
            result = evaluateFunctions(result, rowContext, generationContext, faker);

            // Third pass: Handle any remaining field references (from function outputs)
            result = replaceFieldReferences(result, rowContext, generationContext);

            log.debug("Expression result: {} -> {}", expression, result);
            return result;

        } catch (Exception e) {
            log.error("Error evaluating expression '{}': {}", expression, e.getMessage(), e);
            return "[EXPRESSION_ERROR: " + e.getMessage() + "]";
        }
    }

    /**
     * Replace field references (${fieldName}) with actual values
     */
    private String replaceFieldReferences(
            String expression,
            Map<String, Object> rowContext,
            Map<String, Object> generationContext) {

        Matcher matcher = FIELD_REFERENCE_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String fieldReference = matcher.group(1);
            String replacement = resolveFieldReference(fieldReference, rowContext, generationContext);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Resolve a field reference to its actual value
     */
    private String resolveFieldReference(
            String fieldReference,
            Map<String, Object> rowContext,
            Map<String, Object> generationContext) {

        // Check row context first (field values)
        Object value = rowContext.get(fieldReference);
        if (value != null) {
            return value.toString();
        }

        // Check generation context (metadata)
        value = generationContext.get(fieldReference);
        if (value != null) {
            return value.toString();
        }

        // Special built-in references
        switch (fieldReference.toLowerCase()) {
            case "_row":
            case "_rowindex":
                return generationContext.getOrDefault("_rowIndex", "0").toString();
            case "_timestamp":
                return generationContext.getOrDefault("_timestamp", "").toString();
            default:
                log.warn("Unknown field reference: {}", fieldReference);
                return "[UNKNOWN_FIELD:" + fieldReference + "]";
        }
    }

    /**
     * Evaluate all functions in the expression
     */
    private String evaluateFunctions(
            String expression,
            Map<String, Object> rowContext,
            Map<String, Object> generationContext,
            Faker faker) {

        // Process functions from innermost to outermost to handle nested calls
        String result = expression;
        int maxIterations = 10; // Prevent infinite loops
        int iteration = 0;

        while (containsFunctions(result) && iteration < maxIterations) {
            result = evaluateFunctionsOnce(result, rowContext, generationContext, faker);
            iteration++;
        }

        if (iteration >= maxIterations) {
            log.warn("Maximum function evaluation iterations reached for expression: {}", expression);
        }

        return result;
    }

    /**
     * Check if the expression contains any function calls
     */
    private boolean containsFunctions(String expression) {
        return FUNCTION_PATTERN.matcher(expression).find();
    }

    /**
     * Evaluate functions in the expression once (innermost first)
     */
    private String evaluateFunctionsOnce(
            String expression,
            Map<String, Object> rowContext,
            Map<String, Object> generationContext,
            Faker faker) {

        Matcher matcher = FUNCTION_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String functionName = matcher.group(1);
            String parameters = matcher.group(2);

            String functionResult = evaluateFunction(functionName, parameters, rowContext, generationContext, faker);
            matcher.appendReplacement(result, Matcher.quoteReplacement(functionResult));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Evaluate a single function call
     */
    private String evaluateFunction(
            String functionName,
            String parameters,
            Map<String, Object> rowContext,
            Map<String, Object> generationContext,
            Faker faker) {

        ExpressionFunction function = functions.get(functionName.toLowerCase());
        if (function == null) {
            log.warn("Unknown function: {}", functionName);
            return "[UNKNOWN_FUNCTION:" + functionName + "]";
        }

        try {
            List<String> params = parseParameters(parameters);

            // Create execution context
            FunctionExecutionContext context = new FunctionExecutionContext(
                    rowContext, generationContext, faker, this
            );

            return function.execute(params, context);

        } catch (Exception e) {
            log.error("Error executing function '{}' with params '{}': {}",
                    functionName, parameters, e.getMessage(), e);
            return "[FUNCTION_ERROR:" + functionName + ":" + e.getMessage() + "]";
        }
    }

    /**
     * Parse function parameters, handling quoted strings and nested commas
     */
    private List<String> parseParameters(String parameters) {
        if (parameters == null || parameters.trim().isEmpty()) {
            return Collections.emptyList();
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

        // Remove quotes from parameters
        return params.stream()
                .map(this::removeQuotes)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Remove surrounding quotes from a parameter
     */
    private String removeQuotes(String param) {
        if (param == null || param.length() < 2) {
            return param;
        }

        char first = param.charAt(0);
        char last = param.charAt(param.length() - 1);

        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return param.substring(1, param.length() - 1);
        }

        return param;
    }

    /**
     * Get available function names for documentation/help
     */
    public Set<String> getAvailableFunctions() {
        return new HashSet<>(functions.keySet());
    }

    /**
     * Get function documentation
     */
    public String getFunctionDocumentation(String functionName) {
        ExpressionFunction function = functions.get(functionName.toLowerCase());
        if (function == null) {
            return "Function not found: " + functionName;
        }

        return function.getDescription() + "\nUsage: " + function.getUsage();
    }

    /**
     * Get all function documentation
     */
    public Map<String, String> getAllFunctionDocumentation() {
        Map<String, String> docs = new HashMap<>();
        functions.forEach((name, function) -> {
            docs.put(name, function.getDescription() + "\nUsage: " + function.getUsage());
        });
        return docs;
    }

    /**
     * Validate an expression syntax without evaluating it
     */
    public List<String> validateExpression(String expression) {
        List<String> errors = new ArrayList<>();

        if (expression == null || expression.trim().isEmpty()) {
            return errors;
        }

        try {
            // Check for balanced braces
            if (!hasBalancedBraces(expression)) {
                errors.add("Unbalanced braces in expression");
            }

            // Check for valid field references
            Matcher fieldMatcher = FIELD_REFERENCE_PATTERN.matcher(expression);
            while (fieldMatcher.find()) {
                String fieldRef = fieldMatcher.group(1);
                if (fieldRef.trim().isEmpty()) {
                    errors.add("Empty field reference: ${" + fieldRef + "}");
                }
            }

            // Check for valid function calls
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(expression);
            while (functionMatcher.find()) {
                String functionName = functionMatcher.group(1);
                String parameters = functionMatcher.group(2);

                if (!functions.containsKey(functionName.toLowerCase())) {
                    errors.add("Unknown function: " + functionName);
                }

                // Validate parameter syntax
                try {
                    parseParameters(parameters);
                } catch (Exception e) {
                    errors.add("Invalid parameters for function " + functionName + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("General syntax error: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Check if braces are balanced in the expression
     */
    private boolean hasBalancedBraces(String expression) {
        int braceCount = 0;
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
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount < 0) {
                        return false;
                    }
                }
            }
        }

        return braceCount == 0;
    }
}