package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Pads a string on the left with a specified character
 * Usage: padLeft(value, length, padChar)
 */
public class PadLeftFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "padLeft";
    }

    @Override
    public String getDescription() {
        return "Pads the input value on the left to reach the specified length";
    }

    @Override
    public String getUsage() {
        return "padLeft(value, length, padChar)";
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
            throw new IllegalArgumentException("padLeft requires 2 or 3 parameters: value, length[, padChar]");
        }

        String value = parameters.get(0);

        // If value is a field reference, get it from context
        if (context.hasField(value)) {
            value = context.getFieldValue(value);
        }

        try {
            int targetLength = Integer.parseInt(parameters.get(1).trim());
            char padChar = parameters.size() == 3 ? parameters.get(2).charAt(0) : ' ';

            if (value.length() >= targetLength) {
                return value;
            }

            int paddingNeeded = targetLength - value.length();
            String padding = String.valueOf(padChar).repeat(paddingNeeded);

            return padding + value;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Length parameter must be a valid integer");
        }
    }
}