package com.jpmorgan.reactdemo.generator.impl.address;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component // Register as a Spring bean
public class ZipCodeGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Address.zipCode";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.address().zipCode();
    }

    @Override
    public String getName() {
        return "Zip Code";
    }

    @Override
    public String getCategory() {
        return "Address";
    }
}