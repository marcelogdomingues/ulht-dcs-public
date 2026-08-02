package com.example.dcs.credential.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.dcs.credential.persistence.CredentialStatusEntity;
import com.example.dcs.credential.service.CredentialStatusService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the credential status registry.
 *
 * <p>All routes are served under the service context-path {@code /api/v1} and are
 * protected by the shared {@code apikey} filter (there are no public routes here).
 * See <a href="https://www.w3.org/TR/vc-bitstring-status-list/">W3C Bitstring
 * Status List</a>.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CredentialStatusController {

    /** Identifier of the single default status list this service publishes. */
    private static final String DEFAULT_LIST_ID = "credential-status-list-1";

    private final CredentialStatusService credentialStatusService;

    /** Revoke a credential. Optional body: {@code {"reason": "..."}}. */
    @PostMapping("/credentials/{id}/revoke")
    public ResponseEntity<Map<String, Object>> revoke(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body) {

        String reason = body == null ? null : (String) body.get("reason");
        CredentialStatusEntity entity = credentialStatusService.revoke(id, reason);
        log.info("🚫 Credential revoked: {} (reason={})", id, reason);
        return ResponseEntity.ok(toStatusResponse(entity));
    }

    /** Current status of a credential. */
    @GetMapping("/credentials/{id}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String id) {
        return credentialStatusService.getStatus(id)
                .map(entity -> ResponseEntity.ok(toStatusResponse(entity)))
                .orElseGet(() -> {
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("error", "NOT_FOUND");
                    error.put("message", "No status record for credential: " + id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                });
    }

    /**
     * Returns the Bitstring Status List Verifiable Credential for {@code listId}.
     * A single default list is published; unknown ids yield 404.
     */
    @GetMapping("/status-list/{listId}")
    public ResponseEntity<Map<String, Object>> statusList(@PathVariable String listId) {
        if (!DEFAULT_LIST_ID.equals(listId)) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", "Unknown status list: " + listId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        String encodedList = credentialStatusService.buildEncodedList();
        String listUrl = "urn:uuid:" + listId;

        Map<String, Object> credentialSubject = new LinkedHashMap<>();
        credentialSubject.put("id", listUrl + "#list");
        credentialSubject.put("type", "BitstringStatusList");
        credentialSubject.put("statusPurpose", "revocation");
        credentialSubject.put("encodedList", encodedList);

        Map<String, Object> vc = new LinkedHashMap<>();
        vc.put("@context", List.of(
                "https://www.w3.org/ns/credentials/v2"));
        vc.put("id", listUrl);
        vc.put("type", List.of("VerifiableCredential", "BitstringStatusListCredential"));
        vc.put("issuer", "did:web:credential-service.dcs");
        vc.put("credentialSubject", credentialSubject);

        return ResponseEntity.ok(vc);
    }

    private Map<String, Object> toStatusResponse(CredentialStatusEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("status", entity.getStatus().name());
        response.put("reason", entity.getReason());
        response.put("updatedAt", entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
        return response;
    }
}
