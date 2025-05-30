package com.jpmorgan.reactdemo.generator.impl.string;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class RandomStringGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "String.random";
    }

    @Override
    public String generate(Faker faker, String options) {
        int length = 10; // Default length
        if (options != null && !options.isEmpty()) {
            try {
                length = Integer.parseInt(options.trim());
                if (length < 1) {
                    throw new IllegalArgumentException("Length must be a positive integer.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Options must contain a valid integer for string length.", e);
            }
        }

        return RandomStringUtils.randomAlphanumeric(length);
    }

    @Override
    public String getName() {
        return "Random String";
    }

    @Override
    public String getCategory() {
        return "String";
    }
}