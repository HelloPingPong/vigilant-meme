package com.jpmorgan.reactdemo.generator.impl.contact;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "PhoneNumber.number";
    }

    @Override
    public String generate(Faker faker, String options) {
        //can customize the phone number format using options if needed
        return faker.phoneNumber().phoneNumber();
    }

    @Override
    public String getName() {
        return "Phone Number";
    }

    @Override
    public String getCategory() {
        return "Contact";
    }
}