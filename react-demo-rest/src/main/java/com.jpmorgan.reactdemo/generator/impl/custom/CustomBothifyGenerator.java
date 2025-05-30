package com.jpmorgan.reactdemo.generator.impl.custom;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class CustomBothifyGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Custom.bothify";
    }

    @Override
    public String generate(Faker faker, String options) {
        // Use the options string as a pattern for bothify
        if (options == null || options.isEmpty()) {
            return "";
        }
        return faker.bothify(options);
    }

    @Override
    public String getName() {
        return "Custom Data Type";
    }

    @Override
    public String getCategory() {
        return "Custom";
    }
}