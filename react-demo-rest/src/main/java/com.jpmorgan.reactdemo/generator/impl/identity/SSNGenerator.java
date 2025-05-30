package com.jpmorgan.reactdemo.generator.impl.identity;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class SSNGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Identity.ssn";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.idNumber().ssnValid();
    }

    @Override
    public String getName() {
        return "Social Security Number";
    }

    @Override
    public String getCategory() {
        return "Identity";
    }
}