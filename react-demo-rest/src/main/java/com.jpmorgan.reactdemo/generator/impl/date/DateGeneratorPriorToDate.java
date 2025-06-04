package com.jpmorgan.reactdemo.generator.impl.date;

import com.jpmorgan.reactdemo.generator.DataTypeGenerator;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.TimeZone;

@Component
public class DateGeneratorPriorToDate implements DataTypeGenerator {

    @Override
    public String getKey() {
        return "Date.priorToDate";
    }

    @Override
    public String getName() {
        return "Date Prior to Date Generator";
    }

    @Override
    public String getCategory() {
        return "Date";
    }

    @Override
    public String generate(Faker faker, String options) {
        LocalDate generatedDate = faker.date().past(365, TimeUnit.DAYS).toInstant().atZone(
                ZoneId.of(TimeZone.getDefault().getID())).toLocalDate();
        return generatedDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}