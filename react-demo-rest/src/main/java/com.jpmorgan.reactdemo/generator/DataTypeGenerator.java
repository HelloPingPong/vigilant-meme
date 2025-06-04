package com.jpmorgan.reactdemo.generator;

import com.github.javafaker.Faker;
import java.util.Map;

//TODO: Create currency gen module in impl folder to test format settings and utilize new enhanced gen service

public interface DataTypeGenerator {
    /**
     * Key used to identify this generator (e.g., "Name.firstName").
     * Must be unique.
     */
    String getKey();

    /**
     * User-friendly display name (e.g., "First Name").
     */
    String getName();

    /**
     * Category for grouping (e.g., "Name", "Address").
     */
    String getCategory();

    /**
     * Generates a fake data value.
     * @param faker The Faker instance.
     * @param options Optional configuration for the generator (e.g., date format, regex pattern).
     * @param rowContext Current row context for dependent fields
     * @return The generated fake data as a String.
     */
    default String generate(Faker faker, String options, Map<String, Object> rowContext) {
        // Default implementation ignores rowContext for backward compatibility
        return generate(faker, options);
    }

    /**
     * Generates a fake data value (simplified version).
     * @param faker The Faker instance.
     * @param options Optional configuration for the generator.
     * @return The generated fake data as a String.
     */
    String generate(Faker faker, String options);
}
