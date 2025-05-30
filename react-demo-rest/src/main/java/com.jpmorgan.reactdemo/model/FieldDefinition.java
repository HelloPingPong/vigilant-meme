package com.jpmorgan.reactdemo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
public class FieldDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., "firstName", "email"
    private String dataType; // e.g., "Name.firstName", "Internet.emailAddress" (matches Faker methods)
    private String options; // Optional constraints (JSON string, regex, date format, etc.)
    private int fieldOrder; // To maintain column order

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_definition_id")
    @ToString.Exclude // Avoid circular reference issues in toString
    @EqualsAndHashCode.Exclude // Avoid circular reference issues in equals/hashCode
    private SchemaDefinition schemaDefinition;
}
