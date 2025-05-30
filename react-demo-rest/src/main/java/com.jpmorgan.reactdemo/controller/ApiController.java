package com.jpmorgan.reactdemo.controller;

import com.jpmorgan.reactdemo.dto.*;
import com.jpmorgan.reactdemo.service.DataGenerationService;
import com.jpmorgan.reactdemo.service.SchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping; // Import for DELETE
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api") // Base path for all API endpoints
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow requests from frontend (adjust in production)
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private final DataGenerationService dataGenerationService;
    private final SchemaService schemaService;

    @GetMapping("/datatypes")
    public ResponseEntity<List<DataTypeInfo>> getDataTypes() {
        return ResponseEntity.ok(dataGenerationService.getSupportedDataTypes());
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateData(@RequestBody GenerationRequest request) {
        try {
            log.info("Received generation request: {} rows, format {}", request.getRowCount(), request.getFormat());
            if (request.getSchema() == null || request.getSchema().isEmpty()) {
                return ResponseEntity.badRequest().body("Schema cannot be empty.");
            }
            if (request.getRowCount() <= 0 || request.getRowCount() > 100000) { // Add sensible limits
                return ResponseEntity.badRequest().body("Row count must be between 1 and 100,000.");
            }

            String generatedData = dataGenerationService.generateData(request);
            HttpHeaders headers = new HttpHeaders();
            String filename = "generated_data." + request.getFormat().toLowerCase();
            MediaType mediaType;

            switch (request.getFormat().toUpperCase()) {
                case "CSV":
                    mediaType = MediaType.parseMediaType("text/csv");
                    break;
                case "JSON":
                    mediaType = MediaType.APPLICATION_JSON;
                    break;
                case "XML":
                    mediaType = MediaType.APPLICATION_XML;
                    break;
                case "SQL":
                    mediaType = MediaType.parseMediaType("application/sql");
                    filename = "generated_data.sql";
                    break;
                case "PLAINTEXT":
                    mediaType = MediaType.TEXT_PLAIN;
                    filename = "generated_data.txt";
                    break;
                default:
                    mediaType = MediaType.TEXT_PLAIN; // Fallback
            }

            headers.setContentType(mediaType);
            headers.setContentDispositionFormData("attachment", filename); // Suggest download

            log.info("Successfully generated {} bytes in {} format", generatedData.length(), request.getFormat());
            return new ResponseEntity<>(generatedData, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.warn("Bad request during data generation: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error generating data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred during data generation: " + e.getMessage());
        }
    }

    // Endpoint to generate preview data (avoids sending large vdata for preview)
    @PostMapping("/generate/preview")
    public ResponseEntity<List<Map<String, Object>>> generatePreviewData(@RequestBody GenerationRequest request) {
        try {
            log.info("Received preview generation request: format {}", request.getFormat());
            if (request.getSchema() == null || request.getSchema().isEmpty()) {
                return ResponseEntity.badRequest().build(); // No body needed for bad request
            }
            int previewRowCount = Math.min(Math.max(1, request.getRowCount()), 10); // Generate max 10 rows for preview
            List<Map<String, Object>> previewData = dataGenerationService.generateRawData(request.getSchema(), previewRowCount);
            log.info("Successfully generated {} preview rows", previewData.size());
            return ResponseEntity.ok(previewData);
        } catch (Exception e) {
            log.error("Error generating preview data", e);
            // Don't expose internal errors directly in preview response
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generating preview", e);
        }
    }


    @PostMapping("/schemas")
    public ResponseEntity<SchemaDefinitionDto> saveSchema(@RequestBody SchemaDefinitionDto schemaDto) {
        try {
            SchemaDefinitionDto savedSchema = schemaService.saveSchema(schemaDto);
            log.info("Saved schema with ID: {}", savedSchema.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSchema);
        } catch (Exception e) {
            log.error("Error saving schema", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error saving schema", e);
        }
    }

    // ADD: Endpoint to list all saved schemas (ID and Name) for dropdown
    @GetMapping("/schemas")
    public ResponseEntity<List<SchemaSummaryDto>> getAllSchemaSummaries() {
        List<SchemaSummaryDto> summaries = schemaService.getAllSchemaSummaries();
        return ResponseEntity.ok(summaries);
    }

    // ADD: Endpoint to delete a schema
    @DeleteMapping("/schemas/{id}")
    public ResponseEntity<Void> deleteSchema(@PathVariable Long id) {
        try {
            schemaService.deleteSchema(id);
            log.info("Deleted schema with ID: {}", id);
            return ResponseEntity.noContent().build(); // Standard HTTP 204 No Content on successful delete
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.warn("Attempted to delete non-existent schema with ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema not found", e);
        } catch (Exception e) {
            log.error("Error deleting schema with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting schema", e);
        }
    }

    @GetMapping("/schemas/{id}")
    public ResponseEntity<SchemaDefinitionDto> getSchema(@PathVariable Long id) {
        try {
            SchemaDefinitionDto schemaDto = schemaService.getSchema(id);
            log.info("Retrieved schema with ID: {}", id);
            return ResponseEntity.ok(schemaDto);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.warn("Schema not found with ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema not found", e);
        } catch (Exception e) {
            log.error("Error retrieving schema with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving schema", e);
        }
    }
}