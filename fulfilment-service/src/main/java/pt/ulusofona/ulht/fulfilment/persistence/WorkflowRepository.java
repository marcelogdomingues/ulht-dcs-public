package pt.ulusofona.ulht.fulfilment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for durable workflow status records.
 */
@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowRecord, String> {
}
