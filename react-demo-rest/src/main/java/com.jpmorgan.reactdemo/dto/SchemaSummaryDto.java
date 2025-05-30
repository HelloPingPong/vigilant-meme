package com.jpmorgan.reactdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaSummaryDto {
    private Long id;
    private String name;
}
