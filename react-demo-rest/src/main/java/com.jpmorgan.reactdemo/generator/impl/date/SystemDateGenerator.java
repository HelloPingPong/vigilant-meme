package com.jpmorgan.reactdemo.generator.impl.date;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class SystemDateGenerator implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Date.sameAsSystemDate";
    }

    @Override
    public String getName() {
        return "Date Same As System Date Generator";
    }

    @Override
    public String getCategory() {
        return "Date";
    }

    @Override
    public String generate(Faker faker, String options) {
        LocalDate currentDate = LocalDate.now();
        return currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}