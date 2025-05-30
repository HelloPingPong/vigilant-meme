package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Converts text to lowercase
 * Usage: lowercase(value)
 */
public class LowerCaseFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "lowercase";
    }

    @Override
    public String getDescription() {
        return "Converts the input value to lowercase";
    }

    @Override
    public String getUsage() {
        return "lowercase(value)";
    }

    @Override
    public int getMinParameters() {
        return 1;
    }

    @Override
    public int getMaxParameters() {
        return 1;
    }

    @Override
    public String execute(List<String> parameters, FunctionExecutionContext context) throws IllegalArgumentException {
        if (parameters.size() != 1) {
            throw new IllegalArgumentException("lowercase requires exactly 1 parameter");
        }

        String value = parameters.get(0);

        // If value is a field reference, get it from context
        if (context.hasField(value)) {
            value = context.getFieldValue(value);
        }

        return value.toLowerCase();
    }
}

