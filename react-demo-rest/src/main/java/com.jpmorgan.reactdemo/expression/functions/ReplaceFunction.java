package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Replaces text within a string
 * Usage: replace(value, search, replacement)
 */
public class ReplaceFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "replace";
    }

    @Override
    public String getDescription() {
        return "Replaces all occurrences of search text with replacement text";
    }

    @Override
    public String getUsage() {
        return "replace(value, search, replacement)";
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
            throw new IllegalArgumentException("replace requires exactly 3 parameters: value, search, replacement");
        }

        String value = parameters.get(0);
        String search = parameters.get(1);
        String replacement = parameters.get(2);

        // If value is a field reference, get it from context
        if (context.hasField(value)) {
            value = context.getFieldValue(value);
        }

        return value.replace(search, replacement);
    }
}

