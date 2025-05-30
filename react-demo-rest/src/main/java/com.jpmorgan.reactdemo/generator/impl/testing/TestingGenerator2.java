package com.jpmorgan.reactdemo.generator.impl.testing;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class TestingGenerator2 implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Testing.Generator2";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.address().streetAddress();
    }

    @Override
    public String getName() {
        return "Test 2 - address";
    }

    @Override
    public String getCategory() {
        return "Testing";
    }
}
