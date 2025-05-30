package com.jpmorgan.reactdemo.generator;

import com.github.javafaker.Faker;
import java.util.Collections;
import java.util.Map;

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
     * @return The generated fake data as a String.
     */
    String generate(Faker faker, String options, Map<String, Object> rowContext);

    // Backward compatibility
    default String generate(Faker faker, String options) {
        return generate(faker, options, Collections.emptyMap());
    }


    // New methods for formatting support
    default boolean supportsFormatting() { return false; }
    default String applyFormatting(String value, String formatOptions) { return value; }
}