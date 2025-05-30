package com.jpmorgan.reactdemo.generator.impl.name;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class FirstNameGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Name.firstName";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.name().firstName();
    }

    @Override
    public String getName() {
        return "First Name";
    }

    @Override
    public String getCategory() {
        return "Name";
    }
}

// Add more implementations for Address.streetAddress, Internet.emailAddress,
// Number.randomDigit, Date.past, Business.creditCardNumber, Code.isbn10,
// Custom.regex (needs special handling for options), etc.
// Make sure keys are unique and descriptive.
