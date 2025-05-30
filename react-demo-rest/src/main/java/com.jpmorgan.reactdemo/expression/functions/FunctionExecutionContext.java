package com.jpmorgan.reactdemo.expression.functions;

import com.jpmorgan.reactdemo.expression.ExpressionEvaluator;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.github.javafaker.Faker;
import java.util.Map;

/**
 * Context object passed to expression functions during execution
 * Contains all the information functions need to execute properly
 */
@Data
@AllArgsConstructor
public class FunctionExecutionContext {

    /**
     * Current row context containing values of fields generated so far
     */
    private final Map<String, Object> rowContext;

    /**
     * Generation context containing metadata like row index, timestamp, etc.
     */
    private final Map<String, Object> generationContext;

    /**
     * Faker instance for generating additional random data
     */
    private final Faker faker;

    /**
     * Reference to the expression evaluator for recursive evaluation
     */
    private final ExpressionEvaluator evaluator;

    /**
     * Get a field value from the row context
     * @param fieldName name of the field
     * @return field value as string, or empty string if not found
     */
    public String getFieldValue(String fieldName) {
        Object value = rowContext.get(fieldName);
        return value != null ? value.toString() : "";
    }

    /**
     * Get generation metadata
     * @param key metadata key
     * @return metadata value as string, or empty string if not found
     */
    public String getMetadata(String key) {
        Object value = generationContext.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * Get row index (0-based)
     * @return current row index
     */
    public int getRowIndex() {
        Object rowIndex = generationContext.get("_rowIndex");
        return rowIndex instanceof Integer ? (Integer) rowIndex : 0;
    }

    /**
     * Check if a field exists in the row context
     * @param fieldName name of the field to check
     * @return true if field exists and has a value
     */
    public boolean hasField(String fieldName) {
        return rowContext.containsKey(fieldName) && rowContext.get(fieldName) != null;
    }

    /**
     * Evaluate a nested expression using the expression evaluator
     * @param expression expression to evaluate
     * @return evaluated result
     */
    public String evaluateExpression(String expression) {
        return evaluator.evaluateExpression(expression, rowContext, generationContext, faker);
    }
}