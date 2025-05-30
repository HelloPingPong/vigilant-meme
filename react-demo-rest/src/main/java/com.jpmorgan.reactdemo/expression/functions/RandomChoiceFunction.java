package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Randomly chooses one value from a list of options
 * Usage: randomChoice(option1, option2, option3, ...)
 */
public class RandomChoiceFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "randomChoice";
    }

    @Override
    public String getDescription() {
        return "Randomly selects one value from the provided options";
    }

    @Override
    public String getUsage() {
        return "randomChoice(option1, option2, option3, ...)";
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
            throw new IllegalArgumentException("randomChoice requires at least 2 options");
        }

        // Resolve field references in parameters
        List<String> resolvedOptions = parameters.stream()
                .map(param -> context.hasField(param) ? context.getFieldValue(param) : param)
                .toList();

        int randomIndex = context.getFaker().number().numberBetween(0, resolvedOptions.size());
        return resolvedOptions.get(randomIndex);
    }
}