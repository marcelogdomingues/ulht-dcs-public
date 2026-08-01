package pt.ulusofona.ulht.credential.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for durable issuer sessions.
 */
@Repository
public interface IssuerSessionRepository extends JpaRepository<IssuerSessionEntity, String> {
}
