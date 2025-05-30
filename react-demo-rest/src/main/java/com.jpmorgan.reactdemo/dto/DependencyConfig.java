package com.jpmorgan.reactdemo.dto;

import lombok.Data;
import java.util.List;

@Data
public class DependencyConfig {
    private String expression;
    private List<String> dependsOn;
    private String conditionalLogic;
}
