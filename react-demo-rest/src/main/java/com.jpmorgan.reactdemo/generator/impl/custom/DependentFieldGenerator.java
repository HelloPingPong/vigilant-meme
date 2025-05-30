package com.jpmorgan.reactdemo.generator.impl.custom;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javafaker.Faker;
import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import org.springframework.stereotype.Component;

@Component
public class DependentFieldGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Dependent.expression";
    }

    @Override
    public String generate(Faker faker, String options, Map<String, Object> rowContext) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Dependent field requires expression in options");
        }

        // Parse expression like: "${firstName}_${lastName}_${randomNumber(1000,9999)}"
        return evaluateExpression(options, rowContext, faker);
    }

    private String evaluateExpression(String expression, Map<String, Object> context, Faker faker) {
        // Simple template engine - could be enhanced with SpEL or similar
        String result = expression;

        // Replace field references: ${fieldName}
        Pattern fieldPattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = fieldPattern.matcher(expression);

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            Object value = context.get(fieldName);
            if (value != null) {
                result = result.replace(matcher.group(0), value.toString());
            }
        }

        // Handle functions: ${randomNumber(min,max)}, ${substring(field,start,end)}
        result = evaluateFunctions(result, context, faker);

        return result;
    }
}
