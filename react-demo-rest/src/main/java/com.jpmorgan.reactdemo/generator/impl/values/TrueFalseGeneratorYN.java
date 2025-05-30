package com.jpmorgan.reactdemo.generator.impl.values;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class TrueFalseGeneratorYN implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Boolean.trueFalseYN";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.bool().bool() ? "Y" : "N";
    }

    @Override
    public String getName() {
        return "True/False (Y/N)";
    }

    @Override
    public String getCategory() {
        return "Boolean";
    }
}