package com.jpmorgan.reactdemo.generator.impl.name;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class FullNameGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Name.fullName";
    }

    @Override
    public String generate(Faker faker, String options) {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        boolean includeMiddleInitial = faker.bool().bool();

        if (includeMiddleInitial) {
            char middleInitial = (char) ('A' + faker.random().nextInt(26));
            return String.format("%s %c. %s", firstName, middleInitial, lastName);
        } else {
            return String.format("%s %s", firstName, lastName);
        }
    }

    @Override
    public String getName() {
        return "Full Name";
    }

    @Override
    public String getCategory() {
        return "Name";
    }
}