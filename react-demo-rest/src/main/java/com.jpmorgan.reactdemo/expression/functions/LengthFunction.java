package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Gets the length of a string
 * Usage: length(value)
 */
public class LengthFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "length";
    }

    @Override
    public String getDescription() {
        return "Returns the length of the input value";
    }

    @Override
    public String getUsage() {
        return "length(value)";
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
            throw new IllegalArgumentException("length requires exactly 1 parameter");
        }

        String value = parameters.get(0);

        // If value is a field reference, get it from context
        if (context.hasField(value)) {
            value = context.getFieldValue(value);
        }

        return String.valueOf(value.length());
    }
}