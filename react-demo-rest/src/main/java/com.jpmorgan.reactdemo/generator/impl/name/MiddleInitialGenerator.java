package com.jpmorgan.reactdemo.generator.impl.name;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class MiddleInitialGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Name.middleInitial";
    }

    @Override
    public String generate(Faker faker, String options) {
        char middleInitial = (char) ('A' + faker.random().nextInt(26));
        return String.valueOf(middleInitial);
    }

    @Override
    public String getName() {
        return "Middle Initial";
    }

    @Override
    public String getCategory() {
        return "Name";
    }
}
