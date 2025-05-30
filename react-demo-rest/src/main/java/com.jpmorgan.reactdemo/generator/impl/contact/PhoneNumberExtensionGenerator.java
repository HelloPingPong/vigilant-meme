package com.jpmorgan.reactdemo.generator.impl.contact;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberExtensionGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "PhoneNumber.extension";
    }

    @Override
    public String generate(Faker faker, String options) {
        // Generates a random phone number extension, typically 3 to 5 digits long
        int extensionLength = 3 + faker.random().nextInt(3); // Generates length between 3 and 5
        StringBuilder extension = new StringBuilder();
        for (int i = 0; i < extensionLength; i++) {
            extension.append(faker.number().digit());
        }
        return extension.toString();
    }

    @Override
    public String getName() {
        return "Phone Number Extension";
    }

    @Override
    public String getCategory() {
        return "Contact";
    }
}