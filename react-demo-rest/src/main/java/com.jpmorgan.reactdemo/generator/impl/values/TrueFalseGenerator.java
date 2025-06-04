package com.jpmorgan.reactdemo.generator.impl.values;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class TrueFalseGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Boolean.trueFalse";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.bool().bool() ? "True" : "False";
    }

    @Override
    public String getName() {
        return "True/False";
    }

    @Override
    public String getCategory() {
        return "Boolean";
    }
}