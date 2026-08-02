package com.example.dcs.credential.builder;

import com.example.dcs.credential.dto.waltid.CredentialMapping;
import com.example.dcs.credential.dto.waltid.UniversityDegreeData;
import com.example.dcs.credential.dto.waltid.EducationalIdData;
import com.example.dcs.credential.dto.waltid.EuropeanStudentCardData;

import java.util.*;

/**
 * Builder for W3C Verifiable Credential data structures
 * Supports multiple credential types including University Degrees, Educational IDs, and European Student Cards
 */
public class CredentialDataBuilder {
    
    /**
     * Build University Degree credential data
     */
    public static Map<String, Object> buildUniversityDegreeCredential(
            String credentialId,
            String issuerDid,
            String subjectDid,
            UniversityDegreeData degreeData) {
        
        Map<String, Object> credential = new LinkedHashMap<>();
        
        // @context
        credential.put("@context", Arrays.asList(
            "https://www.w3.org/2018/credentials/v1",
            "https://www.w3.org/2018/credentials/examples/v1"
        ));
        
        // id (can be overridden by mapping)
        if (credentialId != null) {
            credential.put("id", credentialId);
        }
        
        // type
        credential.put("type", Arrays.asList("VerifiableCredential", "UniversityDegree"));
        
        // issuer
        Map<String, Object> issuer = new LinkedHashMap<>();
        issuer.put("id", issuerDid);
        credential.put("issuer", issuer);
        
        // credentialSubject
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("id", subjectDid);
        
        // degree
        Map<String, Object> degree = new LinkedHashMap<>();
        degree.put("type", degreeData.getType());
        degree.put("name", degreeData.getName());
        
        if (degreeData.getField() != null) {
            degree.put("field", degreeData.getField());
        }
        if (degreeData.getInstitution() != null) {
            degree.put("institution", degreeData.getInstitution());
        }
        if (degreeData.getAwardDate() != null) {
            degree.put("awardDate", degreeData.getAwardDate());
        }
        if (degreeData.getGrade() != null) {
            degree.put("grade", degreeData.getGrade());
        }
        
        subject.put("degree", degree);
        credential.put("credentialSubject", subject);
        
        return credential;
    }
    
    /**
     * Build default credential mapping with dynamic values
     */
    public static CredentialMapping buildDefaultMapping() {
        Map<String, Object> issuerMapping = new HashMap<>();
        issuerMapping.put("id", "<issuerDid>");
        
        Map<String, Object> subjectMapping = new HashMap<>();
        subjectMapping.put("id", "<subjectDid>");
        
        return CredentialMapping.builder()
                .id("<uuid>")
                .issuer(issuerMapping)
                .credentialSubject(subjectMapping)
                .issuanceDate("<timestamp>")
                .expirationDate("<timestamp-in:365d>")
                .build();
    }
    
    /**
     * Build custom credential mapping
     */
    public static CredentialMapping buildMapping(
            String idFunction,
            String issuanceDateFunction,
            String expirationDateFunction) {
        
        Map<String, Object> issuerMapping = new HashMap<>();
        issuerMapping.put("id", "<issuerDid>");
        
        Map<String, Object> subjectMapping = new HashMap<>();
        subjectMapping.put("id", "<subjectDid>");
        
        return CredentialMapping.builder()
                .id(idFunction != null ? idFunction : "<uuid>")
                .issuer(issuerMapping)
                .credentialSubject(subjectMapping)
                .issuanceDate(issuanceDateFunction != null ? issuanceDateFunction : "<timestamp>")
                .expirationDate(expirationDateFunction)
                .build();
    }
    
    /**
     * Build generic credential data
     */
    public static Map<String, Object> buildGenericCredential(
            List<String> contexts,
            String credentialId,
            List<String> types,
            String issuerDid,
            String subjectDid,
            Map<String, Object> claims) {
        
        Map<String, Object> credential = new LinkedHashMap<>();
        
        // @context
        credential.put("@context", contexts != null ? contexts : 
            Arrays.asList("https://www.w3.org/2018/credentials/v1"));
        
        // id
        if (credentialId != null) {
            credential.put("id", credentialId);
        }
        
        // type
        List<String> credentialTypes = new ArrayList<>();
        credentialTypes.add("VerifiableCredential");
        if (types != null) {
            credentialTypes.addAll(types);
        }
        credential.put("type", credentialTypes);
        
        // issuer
        Map<String, Object> issuer = new LinkedHashMap<>();
        issuer.put("id", issuerDid);
        credential.put("issuer", issuer);
        
        // credentialSubject
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("id", subjectDid);
        if (claims != null) {
            subject.putAll(claims);
        }
        credential.put("credentialSubject", subject);
        
        return credential;
    }
    
    /**
     * Build Educational ID credential (SCHAC-based)
     */
    public static Map<String, Object> buildEducationalIdCredential(
            String credentialId,
            String issuerDid,
            String subjectDid,
            EducationalIdData eduIdData) {
        
        Map<String, Object> credential = new LinkedHashMap<>();
        
        // @context - include SCHAC context
        credential.put("@context", Arrays.asList(
            "https://www.w3.org/2018/credentials/v1",
            "https://purl.imsglobal.org/spec/clr/v2p0/context-2.0.0.json",
            "https://schema.org"
        ));
        
        // id
        if (credentialId != null) {
            credential.put("id", credentialId);
        }
        
        // type
        credential.put("type", Arrays.asList(
            "VerifiableCredential", 
            "EducationalIdentityCredential"
        ));
        
        // issuer
        Map<String, Object> issuer = new LinkedHashMap<>();
        issuer.put("id", issuerDid);
        credential.put("issuer", issuer);
        
        // credentialSubject
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("id", subjectDid);
        
        // Add all educational ID fields
        if (eduIdData.getIdentifier() != null) {
            subject.put("identifier", eduIdData.getIdentifier());
        }
        if (eduIdData.getSchacPersonalUniqueCode() != null) {
            subject.put("schacPersonalUniqueCode", eduIdData.getSchacPersonalUniqueCode());
        }
        if (eduIdData.getSchacPersonalUniqueID() != null) {
            subject.put("schacPersonalUniqueID", eduIdData.getSchacPersonalUniqueID());
        }
        if (eduIdData.getSchacHomeOrganization() != null) {
            subject.put("schacHomeOrganization", eduIdData.getSchacHomeOrganization());
        }
        if (eduIdData.getFamilyName() != null) {
            subject.put("familyName", eduIdData.getFamilyName());
        }
        if (eduIdData.getFirstName() != null) {
            subject.put("firstName", eduIdData.getFirstName());
        }
        if (eduIdData.getDisplayName() != null) {
            subject.put("displayName", eduIdData.getDisplayName());
        }
        if (eduIdData.getDateOfBirth() != null) {
            subject.put("dateOfBirth", eduIdData.getDateOfBirth());
        }
        if (eduIdData.getCommonName() != null) {
            subject.put("commonName", eduIdData.getCommonName());
        }
        if (eduIdData.getMail() != null) {
            subject.put("mail", eduIdData.getMail());
        }
        if (eduIdData.getEduPersonPrincipalName() != null) {
            subject.put("eduPersonPrincipalName", eduIdData.getEduPersonPrincipalName());
        }
        if (eduIdData.getEduPersonPrimaryAffiliation() != null) {
            subject.put("eduPersonPrimaryAffiliation", eduIdData.getEduPersonPrimaryAffiliation());
        }
        if (eduIdData.getEduPersonAffiliation() != null) {
            subject.put("eduPersonAffiliation", eduIdData.getEduPersonAffiliation());
        }
        if (eduIdData.getEduPersonScopedAffiliation() != null) {
            subject.put("eduPersonScopedAffiliation", eduIdData.getEduPersonScopedAffiliation());
        }
        if (eduIdData.getEduPersonAssurance() != null) {
            subject.put("eduPersonAssurance", eduIdData.getEduPersonAssurance());
        }
        
        credential.put("credentialSubject", subject);
        
        return credential;
    }
    
    /**
     * Build European Student Card credential
     */
    public static Map<String, Object> buildEuropeanStudentCardCredential(
            String credentialId,
            String issuerDid,
            String subjectDid,
            EuropeanStudentCardData escData) {
        
        Map<String, Object> credential = new LinkedHashMap<>();
        
        // @context
        credential.put("@context", Arrays.asList(
            "https://www.w3.org/2018/credentials/v1",
            "https://europa.eu/european-student-card/context/v1",
            "https://schema.org"
        ));
        
        // id
        if (credentialId != null) {
            credential.put("id", credentialId);
        }
        
        // type
        credential.put("type", Arrays.asList(
            "VerifiableCredential", 
            "EuropeanStudentCard"
        ));
        
        // issuer
        Map<String, Object> issuer = new LinkedHashMap<>();
        issuer.put("id", issuerDid);
        if (escData.getInstitutionName() != null) {
            issuer.put("name", escData.getInstitutionName());
        }
        if (escData.getInstitutionPIC() != null) {
            issuer.put("pic", escData.getInstitutionPIC());
        }
        credential.put("issuer", issuer);
        
        // credentialSubject
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("id", subjectDid);
        
        // Student card information
        Map<String, Object> studentCard = new LinkedHashMap<>();
        
        if (escData.getGivenName() != null) {
            studentCard.put("givenName", escData.getGivenName());
        }
        if (escData.getFamilyName() != null) {
            studentCard.put("familyName", escData.getFamilyName());
        }
        if (escData.getEmail() != null) {
            studentCard.put("email", escData.getEmail());
        }
        if (escData.getEsi() != null) {
            studentCard.put("esi", escData.getEsi());
        }
        if (escData.getEscn() != null) {
            studentCard.put("escn", escData.getEscn());
        }
        if (escData.getInstitutionPIC() != null) {
            studentCard.put("institutionPIC", escData.getInstitutionPIC());
        }
        if (escData.getAcademicLevel() != null) {
            studentCard.put("academicLevel", escData.getAcademicLevel());
        }
        if (escData.getCardType() != null) {
            studentCard.put("cardType", escData.getCardType());
        }
        if (escData.getValidFrom() != null) {
            studentCard.put("validFrom", escData.getValidFrom());
        }
        if (escData.getValidUntil() != null) {
            studentCard.put("validUntil", escData.getValidUntil());
        }
        
        subject.put("europeanStudentCard", studentCard);
        credential.put("credentialSubject", subject);
        
        return credential;
    }
}

