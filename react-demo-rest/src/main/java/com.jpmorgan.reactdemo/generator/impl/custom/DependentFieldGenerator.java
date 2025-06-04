package com.jpmorgan.reactdemo.generator.impl.custom;

import java.util.Map;
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
    public String getName() {
        return "Dependent Field";
    }

    @Override
    public String getCategory() {
        return "Custom";
    }

    @Override
    public String generate(Faker faker, String options) {
        // This generator requires row context
        throw new UnsupportedOperationException("DependentFieldGenerator requires row context. Use generate(faker, options, rowContext) instead.");
    }

    @Override
    public String generate(Faker faker, String options, Map<String, Object> rowContext) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Dependent field requires expression in options");
        }
        // For now, return the expression as-is
        // The ExpressionEvaluator will handle this in EnhancedDataGenerationService
        return options;
    }
}
