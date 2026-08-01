package pt.ulusofona.ulht.credential.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for durable issuer session registrations.
 */
@Repository
public interface IssuerRegistrationRepository extends JpaRepository<IssuerRegistrationEntity, String> {

    List<IssuerRegistrationEntity> findBySessionId(String sessionId);

    long countBySessionId(String sessionId);

    boolean existsBySessionIdAndStudentId(String sessionId, String studentId);

    void deleteBySessionId(String sessionId);
}
