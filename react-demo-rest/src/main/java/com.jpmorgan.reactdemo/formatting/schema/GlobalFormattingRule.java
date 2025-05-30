package com.jpmorgan.reactdemo.formatting.schema;

import com.jpmorgan.reactdemo.formatting.enums.CaseTransform;
import lombok.Data;

@Data
public class GlobalFormattingRule {
    private CaseTransform defaultCase;
    private String defaultPrefix;
    private String defaultSuffix;
    private Integer defaultMaxLength;
    private CharacterSet allowedCharacters;
    private String dateFormat; // Applied to all date fields
    private String numberFormat; // Applied to all number fields
}