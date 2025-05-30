package com.jpmorgan.reactdemo.expression.functions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Formats a date/timestamp
 * Usage: formatDate(format) or formatDate(value, format)
 */
public class FormatDateFunction implements ExpressionFunction {

    @Override
    public String getName() {
        return "formatDate";
    }

    @Override
    public String getDescription() {
        return "Formats the current timestamp or a provided date value using the specified format";
    }

    @Override
    public String getUsage() {
        return "formatDate(format) or formatDate(value, format)";
    }

    @Override
    public int getMinParameters() {
        return 1;
    }

    @Override
    public int getMaxParameters() {
        return 2;
    }

    @Override
    public String execute(List<String> parameters, FunctionExecutionContext context) throws IllegalArgumentException {
        if (parameters.isEmpty() || parameters.size() > 2) {
            throw new IllegalArgumentException("formatDate requires 1 or 2 parameters: [value,] format");
        }

        try {
            if (parameters.size() == 1) {
                // Format current timestamp
                String format = parameters.getFirst();
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return now.format(formatter);
            } else {
                // Format provided value
                String value = parameters.get(0);
                String format = parameters.get(1);

                // If value is a field reference, get it from context
                if (context.hasField(value)) {
                    value = context.getFieldValue(value);
                }

                // Try to parse the value as a date/timestamp
                LocalDateTime dateTime = LocalDateTime.parse(value);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return dateTime.format(formatter);
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date value or format: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("Error formatting date: " + e.getMessage());
        }
    }
}