package com.jpmorgan.reactdemo.formatting;

import com.jpmorgan.reactdemo.dto.ValidationRule;
import com.jpmorgan.reactdemo.formatting.enums.CaseTransform;
import com.jpmorgan.reactdemo.formatting.enums.PaddingPosition;
import com.jpmorgan.reactdemo.formatting.enums.TruncatePosition;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Component
@Slf4j
public class FieldFormatter {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Apply all formatting rules to a value
     */
    public String applyFormatting(String value, FieldFormattingOptions options) {
        if (value == null) {
            value = "";
        }

        if (options == null) {
            return value;
        }

        String result = value;

        try {
            // Apply transformations in order:
            // 1. Case transformation
            result = applyCaseTransform(result, options.getCaseTransform());

            // 2. Prefix and suffix
            result = applyPrefixSuffix(result, options.getPrefix(), options.getSuffix());

            // 3. Fixed length formatting (includes padding and truncation)
            result = applyFixedLength(result, options.getFixedLength());

            // 4. Maximum length constraint
            result = applyMaxLength(result, options.getMaxLength());

            // 5. Validation (if validation fails, return error marker)
            if (!validateValue(result, options.getValidation())) {
                log.warn("Value '{}' failed validation", result);
                return "[VALIDATION_FAILED]";
            }

        } catch (Exception e) {
            log.error("Error applying formatting to value '{}': {}", value, e.getMessage(), e);
            return "[FORMAT_ERROR]";
        }

        return result;
    }

    /**
     * Apply case transformation
     */
    private String applyCaseTransform(String value, CaseTransform caseTransform) {
        if (caseTransform == null || value.isEmpty()) {
            return value;
        }

        switch (caseTransform) {
            case UPPER:
                return value.toUpperCase();
            case LOWER:
                return value.toLowerCase();
            case TITLE:
                return toTitleCase(value);
            case CAMEL:
                return toCamelCase(value);
            case PASCAL:
                return toPascalCase(value);
            case SNAKE:
                return toSnakeCase(value);
            case KEBAB:
                return toKebabCase(value);
            default:
                return value;
        }
    }

    /**
     * Convert to Title Case (First Letter Of Each Word Capitalized)
     */
    private String toTitleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : value.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Convert to camelCase
     */
    private String toCamelCase(String value) {
        if (value.isEmpty()) {
            return value;
        }

        String[] words = WHITESPACE_PATTERN.split(value.trim());
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (i == 0) {
                result.append(word);
            } else if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }

        return result.toString();
    }

    /**
     * Convert to PascalCase
     */
    private String toPascalCase(String value) {
        String camelCase = toCamelCase(value);
        if (camelCase.isEmpty()) {
            return camelCase;
        }
        return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1);
    }

    /**
     * Convert to snake_case
     */
    private String toSnakeCase(String value) {
        return WHITESPACE_PATTERN.matcher(value.trim().toLowerCase()).replaceAll("_");
    }

    /**
     * Convert to kebab-case
     */
    private String toKebabCase(String value) {
        return WHITESPACE_PATTERN.matcher(value.trim().toLowerCase()).replaceAll("-");
    }

    /**
     * Apply prefix and suffix
     */
    private String applyPrefixSuffix(String value, String prefix, String suffix) {
        StringBuilder result = new StringBuilder();

        if (prefix != null && !prefix.isEmpty()) {
            result.append(prefix);
        }

        result.append(value);

        if (suffix != null && !suffix.isEmpty()) {
            result.append(suffix);
        }

        return result.toString();
    }

    /**
     * Apply fixed length formatting with padding and truncation
     */
    private String applyFixedLength(String value, FixedLengthConfig config) {
        if (config == null) {
            return value;
        }

        int targetLength = config.getLength();
        if (targetLength <= 0) {
            return value;
        }

        if (value.length() == targetLength) {
            return value;
        }

        if (value.length() > targetLength) {
            // Truncate
            return applyTruncation(value, targetLength, config.getTruncateFrom());
        } else {
            // Pad
            return applyPadding(value, targetLength, config.getPadding());
        }
    }

    /**
     * Apply truncation
     */
    private String applyTruncation(String value, int targetLength, TruncatePosition position) {
        if (position == TruncatePosition.START) {
            return value.substring(value.length() - targetLength);
        } else {
            // Default to END
            return value.substring(0, targetLength);
        }
    }

    /**
     * Apply padding
     */
    private String applyPadding(String value, int targetLength, PaddingConfig padding) {
        if (padding == null) {
            padding = new PaddingConfig(); // Use defaults
        }

        char padChar = padding.getCharacter() != null ? padding.getCharacter() : ' ';
        int paddingNeeded = targetLength - value.length();
        String padString = String.valueOf(padChar).repeat(paddingNeeded);

        if (padding.getPosition() == PaddingPosition.LEFT) {
            return padString + value;
        } else {
            // Default to RIGHT
            return value + padString;
        }
    }

    /**
     * Apply maximum length constraint
     */
    private String applyMaxLength(String value, Integer maxLength) {
        if (maxLength == null || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }

        // Truncate from the end if value exceeds max length
        return value.substring(0, maxLength);
    }

    /**
     * Validate the formatted value
     */
    private boolean validateValue(String value, ValidationRule validation) {
        if (validation == null) {
            return true;
        }

        try {
            // Length validation
            if (validation.getMinLength() != null && value.length() < validation.getMinLength()) {
                return false;
            }
            if (validation.getMaxLength() != null && value.length() > validation.getMaxLength()) {
                return false;
            }

            // Pattern validation
            if (validation.getPattern() != null && !validation.getPattern().isEmpty()) {
                Pattern pattern = Pattern.compile(validation.getPattern());
                if (!pattern.matcher(value).matches()) {
                    return false;
                }
            }

            // Custom validation rules
            if (validation.getCustomRules() != null) {
                for (String rule : validation.getCustomRules()) {
                    if (!evaluateCustomRule(value, rule)) {
                        return false;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error during validation: {}", e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Evaluate custom validation rules
     */
    private boolean evaluateCustomRule(String value, String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return true;
        }

        try {
            rule = rule.toLowerCase().trim();

            // Simple built-in rules
            switch (rule) {
                case "not_empty":
                    return !value.trim().isEmpty();
                case "no_spaces":
                    return !value.contains(" ");
                case "alphanumeric":
                    return value.matches("[a-zA-Z0-9]+");
                case "alphabetic":
                    return value.matches("[a-zA-Z]+");
                case "numeric":
                    return value.matches("[0-9]+");
                case "email_format":
                    return value.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
                case "phone_format":
                    return value.matches("^[+]?[0-9\\-\\(\\)\\s]{10,}$");
                default:
                    log.warn("Unknown validation rule: {}", rule);
                    return true;
            }

        } catch (Exception e) {
            log.warn("Error evaluating custom rule '{}': {}", rule, e.getMessage());
            return true; // Don't fail validation on rule evaluation errors
        }
    }

    /**
     * Format multiple values with the same options (for batch processing)
     */
    public java.util.List<String> applyFormattingBatch(
            java.util.List<String> values,
            FieldFormattingOptions options) {

        return values.stream()
                .map(value -> applyFormatting(value, options))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Preview formatting without applying it (for UI preview)
     */
    public String previewFormatting(String sampleValue, FieldFormattingOptions options) {
        if (sampleValue == null || sampleValue.isEmpty()) {
            sampleValue = "SampleText123";
        }

        return applyFormatting(sampleValue, options);
    }

    /**
     * Get formatting description for display purposes
     */
    public String getFormattingDescription(FieldFormattingOptions options) {
        if (options == null) {
            return "No formatting applied";
        }

        java.util.List<String> descriptions = new java.util.ArrayList<>();

        if (options.getCaseTransform() != null) {
            descriptions.add("Case: " + options.getCaseTransform().name());
        }

        if (options.getPrefix() != null || options.getSuffix() != null) {
            String prefixDesc = options.getPrefix() != null ? "'" + options.getPrefix() + "'" : "";
            String suffixDesc = options.getSuffix() != null ? "'" + options.getSuffix() + "'" : "";
            descriptions.add("Affix: " + prefixDesc + " + value + " + suffixDesc);
        }

        if (options.getFixedLength() != null) {
            descriptions.add("Fixed length: " + options.getFixedLength().getLength() + " chars");
        }

        if (options.getMaxLength() != null) {
            descriptions.add("Max length: " + options.getMaxLength() + " chars");
        }

        if (options.getValidation() != null) {
            descriptions.add("Validation: enabled");
        }

        return descriptions.isEmpty() ? "No formatting applied" : String.join(", ", descriptions);
    }
}
