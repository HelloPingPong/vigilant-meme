package com.jpmorgan.reactdemo.generator.impl.address;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class AddressGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Address.streetAddress";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.address().streetAddress();
    }

    @Override
    public String getName() {
        return "Street Address";
    }

    @Override
    public String getCategory() {
        return "Address";
    }
}