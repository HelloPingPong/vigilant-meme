package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Concatenates multiple values
 * Usage: concat(value1, value2, ...)
 */
public class ConcatFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "concat";
    }

    @Override
    public String getDescription() {
        return "Concatenates multiple values into a single string";
    }

    @Override
    public String getUsage() {
        return "concat(value1, value2, ...)";
    }

    @Override
    public int getMinParameters() {
        return 2;
    }

    @Override
    public int getMaxParameters() {
        return -1; // Unlimited
    }

    @Override
    public String execute(List<String> parameters, FunctionExecutionContext context) throws IllegalArgumentException {
        if (parameters.size() < 2) {
            throw new IllegalArgumentException("concat requires at least 2 parameters");
        }

        StringBuilder result = new StringBuilder();

        for (String param : parameters) {
            String value = param;

            // If value is a field reference, get it from context
            if (context.hasField(value)) {
                value = context.getFieldValue(value);
            }

            result.append(value);
        }

        return result.toString();
    }
}