package com.example.dcs.credential.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the credential status registry.
 */
@Repository
public interface CredentialStatusRepository extends JpaRepository<CredentialStatusEntity, String> {

    /** All credentials in the given status (used to build the Bitstring Status List). */
    List<CredentialStatusEntity> findByStatus(CredentialStatus status);

    /** Highest index currently assigned, or {@code null} when the registry is empty. */
    @Query("select max(c.statusListIndex) from CredentialStatusEntity c")
    Long findMaxIndex();
}
