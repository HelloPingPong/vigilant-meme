package com.jpmorgan.reactdemo.generator.impl.number;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import org.springframework.stereotype.Component;
import com.github.javafaker.Faker;

@Component // Register as a Spring bean
public class IncrementalNumberGenerator implements DataTypeGenerator {

    // Static variable to track the last generated number
    private static int lastGeneratedNumber = 0;

    @Override
    public String getKey() {
        return "Number.incremental";
    }

    @Override
    public String generate(Faker faker, String options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options must contain a valid number range in the format 'min-max'. Optionally, use 'RESET' to restart the sequence.");
        }

        boolean reset = options.contains("RESET");
        options = options.replace("RESET", "").trim(); // Remove the keyword for processing

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

            if (reset || lastGeneratedNumber == 0 || lastGeneratedNumber < min) {
                lastGeneratedNumber = min; // Reset or start from min
            } else {
                lastGeneratedNumber++; // Increment
            }

            if (lastGeneratedNumber > max) {
                throw new IllegalArgumentException("Exceeded maximum value in incremental mode.");
            }

            return String.valueOf(lastGeneratedNumber);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Options must contain valid integers.", e);
        }
    }

    @Override
    public String getName() {
        return "Incremental Number";
    }

    @Override
    public String getCategory() {
        return "Number";
    }
}