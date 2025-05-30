package com.jpmorgan.reactdemo.generator.impl.testing;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class TestingGenerator1 implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "DATA_TYPE_PLACEHOLDER";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.address().streetAddress();
    }

    @Override
    public String getName() {
        return "Placeholder";
    }

    @Override
    public String getCategory() {
        return "Testing";
    }
}
