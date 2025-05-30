package com.jpmorgan.reactdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataTypeInfo {
    private String key; // e.g., "Name.firstName"
    private String name; // e.g., "First Name"
    private String category; // e.g., "Name"
}