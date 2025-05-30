package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Extracts a substring from a field value or string
 * Usage: substring(value, start) or substring(value, start, end)
 */
public class SubstringFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "substring";
    }

    @Override
    public String getDescription() {
        return "Extracts a substring from a value. Start is inclusive, end is exclusive.";
    }

    @Override
    public String getUsage() {
        return "substring(value, start) or substring(value, start, end)";
    }

    @Override
    public int getMinParameters() {
        return 2;
    }

    @Override
    public int getMaxParameters() {
        return 3;
    }

    @Override
    public String execute(List<String> parameters, FunctionExecutionContext context) throws IllegalArgumentException {
        if (parameters.size() < 2 || parameters.size() > 3) {
            throw new IllegalArgumentException("substring requires 2 or 3 parameters: value, start[, end]");
        }

        String value = parameters.get(0);

        // If value is a field reference, get it from context
        if (context.hasField(value)) {
            value = context.getFieldValue(value);
        }

        if (value.isEmpty()) {
            return "";
        }

        try {
            int start = Integer.parseInt(parameters.get(1).trim());

            // Handle negative indices (count from end)
            if (start < 0) {
                start = value.length() + start;
            }

            // Bounds checking for start
            if (start < 0) {
                start = 0;
            }
            if (start >= value.length()) {
                return "";
            }

            if (parameters.size() == 2) {
                // substring(value, start) - from start to end
                return value.substring(start);
            } else {
                // substring(value, start, end)
                int end = Integer.parseInt(parameters.get(2).trim());

                // Handle negative indices (count from end)
                if (end < 0) {
                    end = value.length() + end;
                }

                // Bounds checking for end
                if (end <= start) {
                    return "";
                }
                if (end > value.length()) {
                    end = value.length();
                }

                return value.substring(start, end);
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Start and end parameters must be valid integers");
        }
    }

    @Override
    public List<String> validateParameters(List<String> parameters) {
        if (parameters.size() < 2 || parameters.size() > 3) {
            return List.of("substring requires 2 or 3 parameters");
        }

        try {
            Integer.parseInt(parameters.get(1).trim());
            if (parameters.size() == 3) {
                Integer.parseInt(parameters.get(2).trim());
            }
        } catch (NumberFormatException e) {
            return List.of("Start and end parameters must be valid integers");
        }

        return List.of();
    }
}
