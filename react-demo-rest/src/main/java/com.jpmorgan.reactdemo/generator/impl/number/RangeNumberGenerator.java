package com.jpmorgan.reactdemo.generator.impl.number;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class RangeNumberGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Number.range";
    }

    @Override
    public String generate(Faker faker, String options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options must contain a valid number range in the format 'min-max'.");
        }

        String[] range = options.split("-");
        if (range.length != 2) {
            throw new IllegalArgumentException("Options must contain a valid number range in the format 'min-max'.");
        }

        try {
            int min = Integer.parseInt(range[0].trim());
            int max = Integer.parseInt(range[1].trim());

            if (min > max) {
                throw new IllegalArgumentException("Minimum value cannot be greater than maximum value.");
            }

            return String.valueOf(faker.number().numberBetween(min, max + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Options must contain valid integers.", e);
        }
    }

    @Override
    public String getName() {
        return "Number Range";
    }

    @Override
    public String getCategory() {
        return "Number";
    }
}