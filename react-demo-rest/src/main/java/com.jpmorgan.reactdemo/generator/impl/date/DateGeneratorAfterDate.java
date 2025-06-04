package com.jpmorgan.reactdemo.generator.impl.date;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Component
public class DateGeneratorAfterDate implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Date.afterDate";
    }

    @Override
    public String getName() {
        return "Date After Date Generator";
    }

    @Override
    public String getCategory() {
        return "Date";
    }

    @Override
    public String generate(Faker faker, String options) {
        LocalDate generatedDate = faker.date().future(365, TimeUnit.DAYS).toInstant().atZone(
                ZoneId.of(TimeZone.getDefault().getID())).toLocalDate();
        return generatedDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
