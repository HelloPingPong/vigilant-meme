package com.jpmorgan.reactdemo.generator.impl.address;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class StateGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Address.state";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.address().state();
    }

    @Override
    public String getName() {
        return "State";
    }

    @Override
    public String getCategory() {
        return "Address";
    }
}
