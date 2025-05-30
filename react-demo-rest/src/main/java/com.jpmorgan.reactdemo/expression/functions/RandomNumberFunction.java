package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Generates a random number within a specified range
 * Usage: randomNumber(min, max)
 */
public class RandomNumberFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "randomNumber";
    }

    @Override
    public String getDescription() {
        return "Generates a random integer between min (inclusive) and max (exclusive)";
    }

    @Override
    public String getUsage() {
        return "randomNumber(min, max)";
    }

    @Override
    public int getMinParameters() {
        return 2;
    }

    @Override
    public int getMaxParameters() {
        return 2;
    }

    @Override
    public String execute(List<String> parameters, FunctionExecutionContext context) throws IllegalArgumentException {
        if (parameters.size() != 2) {
            throw new IllegalArgumentException("randomNumber requires exactly 2 parameters: min, max");
        }

        try {
            int min = Integer.parseInt(parameters.get(0).trim());
            int max = Integer.parseInt(parameters.get(1).trim());

            if (min >= max) {
                throw new IllegalArgumentException("min must be less than max");
            }

            int randomValue = context.getFaker().number().numberBetween(min, max);
            return String.valueOf(randomValue);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Both parameters must be valid integers");
        }
    }

    @Override
    public List<String> validateParameters(List<String> parameters) {
        if (parameters.size() != 2) {
            return List.of("randomNumber requires exactly 2 parameters");
        }

        try {
            int min = Integer.parseInt(parameters.get(0).trim());
            int max = Integer.parseInt(parameters.get(1).trim());

            if (min >= max) {
                return List.of("min parameter must be less than max parameter");
            }
        } catch (NumberFormatException e) {
            return List.of("Both parameters must be valid integers");
        }

        return List.of();
    }
}
