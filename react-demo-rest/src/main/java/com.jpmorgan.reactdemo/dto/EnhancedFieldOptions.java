package com.jpmorgan.reactdemo.dto;

import com.jpmorgan.reactdemo.formatting.FieldFormattingOptions;
import com.jpmorgan.reactdemo.formatting.PaddingConfig;
import com.jpmorgan.reactdemo.formatting.enums.CaseTransform;
import com.jpmorgan.reactdemo.formatting.enums.TruncatePosition;
import lombok.Data;

import java.util.List;

@Data
public class EnhancedFieldOptions {
    private String baseType;           // Original generator type
    private Object baseOptions;        // Original options
    private FieldFormattingOptions formatting;
    private DependencyConfig dependency;

    @Data
    public static class FieldFormattingOptions {
        private FixedLengthConfig fixedLength;
        private CaseTransform caseTransform;
        private String prefix;
        private String suffix;
        private ValidationRule validation;
    }

    @Data
    public static class FixedLengthConfig {
        private int length;
        private TruncatePosition truncateFrom = TruncatePosition.END;
        private PaddingConfig padding = new PaddingConfig();
    }

    @Data
    public static class DependencyConfig {
        private String expression;
        private List<String> dependsOn;
        private String conditionalLogic;
    }
}