package com.example.dcs.credential.mapper;

import lombok.extern.slf4j.Slf4j;
import com.example.dcs.credential.dto.EducationalIdCredentialRequest;
import com.example.dcs.credential.dto.EuropeanStudentCardRequest;
import com.example.dcs.credential.dto.UniversityDegreeCredentialRequest;

import java.util.Arrays;
import java.util.Map;

/**
 * Maps student data from SIS Service to credential requests
 */
@Slf4j
public class StudentDataMapper {
    
    private static final String DEFAULT_HOME_ORGANIZATION = "usis.pt";
    private static final String DEFAULT_INSTITUTION_NAME = "DCS - Example University";
    private static final String DEFAULT_INSTITUTION_PIC = "997605425";
    
    /**
     * Converts student data to Educational ID credential request
     */
    public static EducationalIdCredentialRequest toEducationalIdRequest(
            Map<String, Object> studentData,
            String subjectDid,
            String issuerDid,
            Object issuerKey) {
        
        // Extract student information
        String studentId = extractString(studentData, "studentId", "studentCode", "id");
        String fullName = extractString(studentData, "name", "fullName", "displayName");
        String email = extractString(studentData, "email", "mail");
        String dateOfBirth = extractString(studentData, "dateOfBirth", "birthDate");
        
        // Parse name into first and last
        String[] nameParts = parseFullName(fullName);
        String firstName = nameParts[0];
        String familyName = nameParts[1];
        
        // Extract or generate email if not present
        if (email == null || email.isEmpty()) {
            email = generateStudentEmail(studentId, firstName, familyName);
        }
        
        return EducationalIdCredentialRequest.builder()
                .identifier(studentId)
                .familyName(familyName)
                .firstName(firstName)
                .mail(email)
                .dateOfBirth(dateOfBirth)
                .schacHomeOrganization(DEFAULT_HOME_ORGANIZATION)
                .eduPersonPrimaryAffiliation("student")
                .eduPersonAffiliation(Arrays.asList("student"))
                .eduPersonAssurance(Arrays.asList("https://refeds.org/assurance/IAP/low"))
                .subjectDid(subjectDid)
                .issuerDid(issuerDid)
                .issuerKey(issuerKey)
                .build();
    }
    
    /**
     * Converts student data to European Student Card request
     */
    public static EuropeanStudentCardRequest toEuropeanStudentCardRequest(
            Map<String, Object> studentData,
            String subjectDid,
            String issuerDid,
            Object issuerKey) {
        
        String studentId = extractString(studentData, "studentId", "studentCode", "id");
        String fullName = extractString(studentData, "name", "fullName", "displayName");
        String email = extractString(studentData, "email", "mail");
        
        String[] nameParts = parseFullName(fullName);
        String givenName = nameParts[0];
        String familyName = nameParts[1];
        
        if (email == null || email.isEmpty()) {
            email = generateStudentEmail(studentId, givenName, familyName);
        }
        
        // Generate ESI and ESCN
        String esi = generateESI(studentId);
        String escn = studentId;
        
        // Extract academic level from course/degree data if available
        String academicLevel = determineAcademicLevel(studentData);
        
        return EuropeanStudentCardRequest.builder()
                .givenName(givenName)
                .familyName(familyName)
                .email(email)
                .esi(esi)
                .escn(escn)
                .institutionPIC(DEFAULT_INSTITUTION_PIC)
                .institutionName(DEFAULT_INSTITUTION_NAME)
                .academicLevel(academicLevel)
                .cardType("Digital")
                .validFrom(java.time.LocalDate.now().toString())
                .validUntil(java.time.LocalDate.now().plusYears(1).toString())
                .subjectDid(subjectDid)
                .issuerDid(issuerDid)
                .issuerKey(issuerKey)
                .build();
    }
    
    /**
     * Converts student data to University Degree credential request
     */
    public static UniversityDegreeCredentialRequest toUniversityDegreeRequest(
            Map<String, Object> studentData,
            String subjectDid,
            String issuerDid,
            Object issuerKey) {
        
        String studentId = extractString(studentData, "studentId", "studentCode", "id");
        String degreeName = extractString(studentData, "degreeName", "courseName");
        String field = extractString(studentData, "department", "courseArea");
        
        // Extract degree type and details
        String degreeType = determineDegreeType(studentData);
        if (degreeName == null) {
            degreeName = "Bachelor of Science";  // Default
        }
        
        return UniversityDegreeCredentialRequest.builder()
                .studentId(studentId)
                .degreeName(degreeName)
                .degreeType(degreeType)
                .field(field)
                .universityName(DEFAULT_INSTITUTION_NAME)
                .graduationDate(java.time.LocalDate.now().toString())
                .subjectDid(subjectDid)
                .issuerDid(issuerDid)
                .issuerKey(issuerKey)
                .build();
    }
    
    /**
     * Helper: Extract string from map with fallback keys
     */
    public static String extractString(Map<String, Object> data, String... keys) {
        if (data == null) return null;
        
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
    
    /**
     * Helper: Parse full name into first and family name
     */
    private static String[] parseFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[]{"Student", "User"};
        }
        
        String[] parts = fullName.trim().split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return new String[]{parts[0], parts[1]};
    }
    
    /**
     * Helper: Generate student email
     * Normalizes studentId to ensure 'a' prefix is present
     */
    private static String generateStudentEmail(String studentId, String firstName, String familyName) {
        if (studentId != null && !studentId.isEmpty()) {
            // Normalize: Ensure 'a' prefix is present (e.g., "22102286" -> "a12345678")
            String normalizedId = normalizeStudentId(studentId);
            return normalizedId + "@students.example.edu";
        }
        
        String emailPrefix = (firstName + "." + familyName).toLowerCase()
                .replaceAll("\\s+", ".")
                .replaceAll("[áàâã]", "a")
                .replaceAll("[éèê]", "e")
                .replaceAll("[íì]", "i")
                .replaceAll("[óòôõ]", "o")
                .replaceAll("[úù]", "u")
                .replaceAll("[ç]", "c");
        
        return emailPrefix + "@students.example.edu";
    }
    
    /**
     * Normalizes student ID by ensuring 'a' prefix is present
     * Adds 'a' prefix if missing, keeps it if already present
     * 
     * @param studentId Student ID (e.g., "22102286" or "a12345678")
     * @return Normalized student ID with 'a' prefix (e.g., "a12345678")
     */
    public static String normalizeStudentId(String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            return studentId;
        }
        
        // If it already starts with 'a' (case-insensitive), return as-is
        if (studentId.length() > 0 && studentId.toLowerCase().startsWith("a")) {
            return studentId;
        }
        
        // If it's numeric, add 'a' prefix
        if (studentId.matches("\\d+")) {
            return "a" + studentId;
        }
        
        // If it doesn't match expected format, return as-is (might be a different format)
        return studentId;
    }
    
    /**
     * Helper: Generate European Student Identifier
     */
    private static String generateESI(String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            return "ESI-" + System.currentTimeMillis();
        }
        
        // Generate ESI from student ID
        long hash = studentId.hashCode() & 0xFFFFFFFFL;  // Positive number
        return String.format("ESI-%010d", hash);
    }
    
    /**
     * Helper: Determine academic level from student data
     */
    private static String determineAcademicLevel(Map<String, Object> studentData) {
        if (studentData == null) return "bachelor";
        
        String degreeCode = extractString(studentData, "degreeCode", "courseCode");
        String courseName = extractString(studentData, "courseName", "degreeName");
        
        if (courseName != null) {
            String lower = courseName.toLowerCase();
            if (lower.contains("master") || lower.contains("mestrado")) {
                return "master";
            }
            if (lower.contains("doctor") || lower.contains("doutoramento") || lower.contains("phd")) {
                return "doctorate";
            }
        }
        
        return "bachelor";  // Default
    }
    
    /**
     * Helper: Determine degree type
     */
    private static String determineDegreeType(Map<String, Object> studentData) {
        String academicLevel = determineAcademicLevel(studentData);
        
        switch (academicLevel) {
            case "master":
                return "MasterDegree";
            case "doctorate":
                return "DoctoralDegree";
            default:
                return "BachelorDegree";
        }
    }
}

