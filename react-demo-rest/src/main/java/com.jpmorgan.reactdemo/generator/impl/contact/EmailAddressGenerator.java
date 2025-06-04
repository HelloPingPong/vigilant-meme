package com.jpmorgan.reactdemo.generator.impl.contact;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class EmailAddressGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Internet.emailAddress";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.internet().emailAddress();
    }

    @Override
    public String getName() {
        return "Email Address";
    }

    @Override
    public String getCategory() {
        return "Internet";
    }
}