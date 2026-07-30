package pt.ulusofona.ulht.credential.service.issuer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.ulusofona.ulht.credential.domain.issuer.Session;
import pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest;
import pt.ulusofona.ulht.credential.dto.waltid.IssuerKey;
import pt.ulusofona.ulht.credential.exception.ExternalServiceException;
import pt.ulusofona.ulht.credential.service.IssuerKeyService;
import pt.ulusofona.ulht.credential.service.StudentWalletService;
import pt.ulusofona.ulht.credential.service.WaltidService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for issuing session credentials to students.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCredentialService {
    
    private final StudentWalletService studentWalletService;
    private final WaltidService waltidService;
    private final IssuerKeyService issuerKeyService;
    
    /**
     * Issues a session credential to a student.
     */
    public Map<String, Object> issueSessionCredential(String sessionId, Session session, String studentId) 
            throws ExternalServiceException {
        
        log.info("Issuing session credential for student {} to session {}", studentId, sessionId);
        
        try {
            // 1. Get student wallet info
            Map<String, Object> studentData = Map.of(
                "studentId", studentId,
                "email", studentId + "@students.example.edu"
            );
            
            StudentWalletService.StudentWalletInfo walletInfo = 
                studentWalletService.ensureWalletExists(studentData);
            
            String subjectDid = walletInfo.getSubjectDid();
            if (subjectDid == null || subjectDid.isEmpty()) {
                throw new ExternalServiceException(
                    "Student wallet does not have a DID. Please issue base credentials first.", 
                    "SessionCredentialService");
            }
            
            // 2. Get issuer credentials
            String issuerDid = issuerKeyService.getIssuerDid();
            Object issuerKeyObj = issuerKeyService.getIssuerKey();
            IssuerKey issuerKey = convertToIssuerKey(issuerKeyObj);
            
            // 3. Build credential data
            Map<String, Object> credentialData = buildSessionCredentialData(session, studentId);
            
            // 4. Build credential mapping
            Map<String, Object> subjectMapping = new HashMap<>();
            subjectMapping.put("id", "<subjectDid>");
            
            pt.ulusofona.ulht.credential.dto.waltid.CredentialMapping mapping = 
                pt.ulusofona.ulht.credential.dto.waltid.CredentialMapping.builder()
                    .id("<uuid>")
                    .credentialSubject(subjectMapping)
                    .issuanceDate("<timestamp>")
                    .expirationDate("<timestamp-in:365d>")
                    .build();
            
            // 5. Create issuance request
            IssueCredentialRequest request = IssueCredentialRequest.builder()
                .issuerKey(issuerKey)
                .issuerDid(issuerDid)
                .credentialConfigurationId("UniversityDegree_jwt_vc_json") // Use existing config for now
                .credentialData(credentialData)
                .mapping(mapping)
                .authenticationMethod("PRE_AUTHORIZED")
                .standardVersion("DRAFT13")
                .build();
            
            // 6. Issue credential
            String offerUrl = waltidService.issueSdJwtCredential(request);
            
            // 7. Accept credential into wallet
            try {
                waltidService.acceptCredentialOffer(
                    walletInfo.getWalletId(),
                    subjectDid,
                    offerUrl,
                    walletInfo.getSessionCookie(),
                    walletInfo.getEmail()
                );
                log.info("✅ Session credential accepted into wallet for student {}", studentId);
            } catch (Exception e) {
                log.warn("⚠️  Credential issued but could not be automatically accepted into wallet: {}", 
                    e.getMessage());
                // Continue - credential was issued, user can accept manually
            }
            
            return Map.of(
                "success", true,
                "sessionId", sessionId,
                "studentId", studentId,
                "credentialOfferUrl", offerUrl,
                "message", "Session credential issued successfully"
            );
            
        } catch (Exception e) {
            log.error("Failed to issue session credential: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                "Failed to issue session credential: " + e.getMessage(), 
                "SessionCredentialService");
        }
    }
    
    private Map<String, Object> buildSessionCredentialData(Session session, String studentId) {
        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put("id", "<subjectDid>");
        credentialSubject.put("studentId", studentId);
        credentialSubject.put("sessionId", session.getId());
        credentialSubject.put("sessionTitle", session.getTitle());
        credentialSubject.put("conferenceName", session.getConferenceName());
        
        if (session.getDescription() != null) {
            credentialSubject.put("description", session.getDescription());
        }
        if (session.getLocation() != null) {
            credentialSubject.put("location", session.getLocation());
        }
        if (session.getStartTime() != null) {
            credentialSubject.put("startTime", session.getStartTime().toString());
        }
        if (session.getEndTime() != null) {
            credentialSubject.put("endTime", session.getEndTime().toString());
        }
        
        credentialSubject.put("registrationDate", Instant.now().toString());
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("credentialSubject", credentialSubject);
        credentialData.put("@context", new String[]{
            "https://www.w3.org/2018/credentials/v1",
            "https://example.org/credentials/session/v1"
        });
        // Use conference name in the credential type for better display
        String conferenceType = "ConferenceSession_" + session.getConferenceName()
            .replaceAll("[^a-zA-Z0-9]", "")
            .replaceAll("\\s+", "");
        
        credentialData.put("type", new String[]{
            "VerifiableCredential", 
            "ConferenceSessionCredential",
            conferenceType
        });
        
        // Add display name for wallet
        credentialData.put("name", session.getConferenceName() + " - " + session.getTitle());
        credentialData.put("displayName", session.getConferenceName());
        
        return credentialData;
    }
    
    private IssuerKey convertToIssuerKey(Object issuerKeyObj) {
        if (issuerKeyObj instanceof IssuerKey) {
            return (IssuerKey) issuerKeyObj;
        }
        // Handle other cases if needed
        throw new IllegalArgumentException("Invalid issuer key type: " + 
            (issuerKeyObj != null ? issuerKeyObj.getClass().getName() : "null"));
    }
}

