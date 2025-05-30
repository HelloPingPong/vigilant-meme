package com.jpmorgan.reactdemo.expression.functions;

import java.util.List;

/**
 * Base interface for all expression functions that can be used in field expressions
 */
public interface ExpressionFunction {

    /**
     * The name of the function as it appears in expressions
     * @return function name (e.g., "randomNumber", "substring")
     */
    String getName();

    /**
     * Human-readable description of what this function does
     * @return description text
     */
    String getDescription();

    /**
     * Usage example/syntax for this function
     * @return usage string (e.g., "randomNumber(min, max)")
     */
    String getUsage();

    /**
     * Execute the function with given parameters
     * @param parameters list of string parameters
     * @param context execution context containing row data, faker, etc.
     * @return result of function execution
     * @throws IllegalArgumentException if parameters are invalid
     */
    String execute(List<String> parameters, FunctionExecutionContext context) throws IllegalArgumentException;

    /**
     * Validate parameters without executing (optional, for early validation)
     * @param parameters list of parameters to validate
     * @return list of validation error messages (empty if valid)
     */
    default List<String> validateParameters(List<String> parameters) {
        return List.of(); // Default: no validation errors
    }

    /**
     * Get the minimum number of required parameters
     * @return minimum parameter count
     */
    default int getMinParameters() {
        return 0;
    }

    /**
     * Get the maximum number of allowed parameters (-1 for unlimited)
     * @return maximum parameter count
     */
    default int getMaxParameters() {
        return -1; // Unlimited by default
    }
}