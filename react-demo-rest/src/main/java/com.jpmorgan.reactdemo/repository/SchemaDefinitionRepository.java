package com.jpmorgan.reactdemo.repository;

import com.jpmorgan.reactdemo.model.SchemaDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaDefinitionRepository extends JpaRepository<SchemaDefinition, Long> {
}
