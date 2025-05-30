package com.jpmorgan.reactdemo.formatting;

import com.jpmorgan.reactdemo.dto.ValidationRule;
import com.jpmorgan.reactdemo.formatting.enums.CaseTransform;
import lombok.Data;

@Data
public class FieldFormattingOptions {
    private FixedLengthConfig fixedLength;
    private CaseTransform caseTransform;
    private String prefix;
    private String suffix;
    private ValidationRule validation;
}