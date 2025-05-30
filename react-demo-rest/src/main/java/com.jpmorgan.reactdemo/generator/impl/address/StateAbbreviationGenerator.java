package com.jpmorgan.reactdemo.generator.impl.address;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class StateAbbreviationGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Address.stateAbbreviation";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.address().stateAbbr();
    }

    @Override
    public String getName() {
        return "State Abbreviation";
    }

    @Override
    public String getCategory() {
        return "Address";
    }
}