package com.jpmorgan.reactdemo.generator.impl.name;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class LastNameGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Name.lastName";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.name().lastName();
    }

    @Override
    public String getName() {
        return "Last Name";
    }

    @Override
    public String getCategory() {
        return "Name";
    }
}