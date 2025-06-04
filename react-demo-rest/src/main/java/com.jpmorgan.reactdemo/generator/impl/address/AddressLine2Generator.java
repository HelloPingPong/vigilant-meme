package com.jpmorgan.reactdemo.generator.impl.address;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class AddressLine2Generator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Address.addressLine2";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.address().secondaryAddress();
    }

    @Override
    public String getName() {
        return "StreetAddress Line 2";
    }

    @Override
    public String getCategory() {
        return "Address";
    }
}
