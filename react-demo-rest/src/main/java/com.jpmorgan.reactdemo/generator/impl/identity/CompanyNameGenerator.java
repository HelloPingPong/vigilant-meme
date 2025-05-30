package com.jpmorgan.reactdemo.generator.impl.identity;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

@Component
public class CompanyNameGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Business.companyName";
    }

    @Override
    public String generate(Faker faker, String options) {
        return faker.company().name();
    }

    @Override
    public String getName() {
        return "Company Name";
    }

    @Override
    public String getCategory() {
        return "Business";
    }
}