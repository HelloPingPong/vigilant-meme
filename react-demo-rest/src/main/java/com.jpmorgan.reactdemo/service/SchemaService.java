package com.jpmorgan.reactdemo.service;

import com.jpmorgan.reactdemo.dto.FieldDefinitionDto;
import com.jpmorgan.reactdemo.dto.SchemaDefinitionDto;
import com.jpmorgan.reactdemo.model.FieldDefinition;
import com.jpmorgan.reactdemo.model.SchemaDefinition;
import com.jpmorgan.reactdemo.repository.SchemaDefinitionRepository;
import com.jpmorgan.reactdemo.dto.SchemaSummaryDto;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final SchemaDefinitionRepository schemaRepository;
    private static final Logger log = LoggerFactory.getLogger(SchemaService.class);

    @Transactional // Ensure atomicity
    public SchemaDefinitionDto saveSchema(SchemaDefinitionDto schemaDto) {
        SchemaDefinition schema = new SchemaDefinition();
        schema.setName(schemaDto.getName());

        // Use IntStream to set the order correctly
        List<FieldDefinition> fields = IntStream.range(0, schemaDto.getFields().size())
                .mapToObj(i -> {
                    FieldDefinitionDto fieldDto = schemaDto.getFields().get(i);
                    FieldDefinition field = new FieldDefinition();
                    field.setName(fieldDto.getName());
                    field.setDataType(fieldDto.getDataType());
                    field.setOptions(fieldDto.getOptions());
                    field.setFieldOrder(i); // Set the order
                    // The relationship is managed by SchemaDefinition.addField
                    // field.setSchemaDefinition(schema); // Set manually before save if not using cascade helper
                    return field;
                })
                .collect(Collectors.toList());

        // Add fields using the helper method to ensure bidirectional link is set
        fields.forEach(schema::addField);

        SchemaDefinition savedSchema = schemaRepository.save(schema);
        return convertToDto(savedSchema);
    }

    @Transactional(readOnly = true)
    public SchemaDefinitionDto getSchema(Long id) {
        SchemaDefinition schema = schemaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Schema not found with id: " + id));
        return convertToDto(schema);
    }

    private SchemaDefinitionDto convertToDto(SchemaDefinition schema) {
        SchemaDefinitionDto dto = new SchemaDefinitionDto();
        dto.setId(schema.getId());
        dto.setName(schema.getName());
        dto.setFields(schema.getFields().stream()
                // .sorted(Comparator.comparingInt(FieldDefinition::getFieldOrder)) // Ensure order if not using @OrderBy
                .map(this::convertFieldToDto)
                .collect(Collectors.toList()));
        dto.setShareLink(generateShareLink(schema.getId())); // Generate share link
        return dto;
    }

    private FieldDefinitionDto convertFieldToDto(FieldDefinition field) {
        FieldDefinitionDto dto = new FieldDefinitionDto();
        dto.setName(field.getName());
        dto.setDataType(field.getDataType());
        dto.setOptions(field.getOptions());
        //might need id if UI needs to update specific fields later
        return dto;
    }

    private String generateShareLink(Long schemaId) {
        // Generates a link relative to the current request's base URL
        // Assumes the API is mounted at the root or a known context path
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/schemas/{id}") // Matches GET endpoint
                .buildAndExpand(schemaId)
                .toUriString();
    }

    @Transactional(readOnly = true)
    public List<SchemaSummaryDto> getAllSchemaSummaries() {
        log.debug("Fetching all schema summaries");
        return schemaRepository.findAll().stream()
                .map(schema -> new SchemaSummaryDto(schema.getId(), schema.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSchema(Long id) {
        log.debug("Attempting to delete schema with id: {}", id);
        if (!schemaRepository.existsById(id)) {
            throw new EntityNotFoundException("Schema not found with id: " + id);
        }
        schemaRepository.deleteById(id);
        log.info("Successfully deleted schema with id: {}", id);
    }
}


