package com.example.dcs.credential.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Kafka idempotency guard.
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
