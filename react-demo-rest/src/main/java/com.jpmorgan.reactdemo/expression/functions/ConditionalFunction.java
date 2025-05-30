package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Conditional function that returns different values based on a condition
 * Usage: conditional(condition, valueIfTrue, valueIfFalse)
 */
public class ConditionalFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "conditional";
    }

    @Override
    public String getDescription() {
        return "Returns different values based on a condition evaluation";
    }

    @Override
    public String getUsage() {
        return "conditional(condition, valueIfTrue, valueIfFalse)";
    }

    @Override
    public int getMinParameters() {
        return 3;
    }

    @Override
    public int getMaxParameters() {
        return 3;
    }

    @Override
    public String execute(List<String> parameters, FunctionExecutionContext context) throws IllegalArgumentException {
        if (parameters.size() != 3) {
            throw new IllegalArgumentException("conditional requires exactly 3 parameters: condition, valueIfTrue, valueIfFalse");
        }

        String condition = parameters.get(0);
        String valueIfTrue = parameters.get(1);
        String valueIfFalse = parameters.get(2);

        // Resolve field references
        if (context.hasField(condition)) {
            condition = context.getFieldValue(condition);
        }
        if (context.hasField(valueIfTrue)) {
            valueIfTrue = context.getFieldValue(valueIfTrue);
        }
        if (context.hasField(valueIfFalse)) {
            valueIfFalse = context.getFieldValue(valueIfFalse);
        }

        // Simple condition evaluation
        boolean conditionResult = evaluateCondition(condition, context);

        return conditionResult ? valueIfTrue : valueIfFalse;
    }

    private boolean evaluateCondition(String condition, FunctionExecutionContext context) {
        // Simple condition evaluation - can be enhanced
        condition = condition.trim().toLowerCase();

        // Handle boolean literals
        if ("true".equals(condition) || "1".equals(condition)) {
            return true;
        }
        if ("false".equals(condition) || "0".equals(condition)) {
            return false;
        }

        // Handle row index conditions
        if (condition.startsWith("rowindex")) {
            try {
                int rowIndex = context.getRowIndex();
                if (condition.contains("even")) {
                    return rowIndex % 2 == 0;
                }
                if (condition.contains("odd")) {
                    return rowIndex % 2 == 1;
                }
                if (condition.contains(">")) {
                    String[] parts = condition.split(">");
                    int threshold = Integer.parseInt(parts[1].trim());
                    return rowIndex > threshold;
                }
                if (condition.contains("<")) {
                    String[] parts = condition.split("<");
                    int threshold = Integer.parseInt(parts[1].trim());
                    return rowIndex < threshold;
                }
            } catch (Exception e) {
                // Fall through to default
            }
        }

        // Default: non-empty string is true
        return !condition.isEmpty();
    }
}