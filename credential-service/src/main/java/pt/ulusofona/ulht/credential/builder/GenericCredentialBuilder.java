package pt.ulusofona.ulht.credential.builder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import pt.ulusofona.ulht.credential.config.CredentialTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Generic builder that creates W3C Verifiable Credentials from templates
 * Supports any credential type configured in application.yml
 */
@Slf4j
@Component
public class GenericCredentialBuilder {
    
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    /**
     * Builds a W3C credential from a template and student data
     * 
     * @param template The credential template configuration
     * @param studentData Raw student data from Lusofona Service
     * @param subjectDid The DID of the credential subject (student)
     * @param issuerDid The DID of the credential issuer (university)
     * @return W3C credential data ready for issuance
     */
    public Map<String, Object> buildCredential(
            CredentialTemplate template,
            Map<String, Object> studentData,
            String subjectDid,
            String issuerDid) {
        
        log.debug("Building credential from template: {} (type: {})", 
            template.getId(), template.getType());
        
        Map<String, Object> credential = new LinkedHashMap<>();
        
        // 1. Add @context
        List<String> contexts = template.getContexts() != null ? 
            new ArrayList<>(template.getContexts()) :
            List.of("https://www.w3.org/2018/credentials/v1");
        credential.put("@context", contexts);
        
        // 2. Add type
        List<String> types = new ArrayList<>();
        types.add("VerifiableCredential");
        if (template.getAdditionalTypes() != null) {
            types.addAll(template.getAdditionalTypes());
        }
        types.add(template.getType());
        credential.put("type", types);
        
        // 3. Add issuer (as object per W3C VC spec)
        Map<String, Object> issuer = new LinkedHashMap<>();
        issuer.put("id", issuerDid);
        credential.put("issuer", issuer);
        
        // 4. Add issuance date (ISO-8601 with UTC timezone)
        // The mapping uses <timestamp> which tells WaltID to generate the current timestamp
        // WaltID will use the mapping to set this field, but we also set it here for structure
        String issuanceDate = Instant.now().toString();
        credential.put("issuanceDate", issuanceDate);
        log.debug("Set issuance date: {} (mapping will use <timestamp> to generate actual value)", issuanceDate);
        
        // 5. Add expiration date (1 year from now, matching the mapping)
        // The mapping uses <timestamp-in:365d> which tells WaltID to generate timestamp + 365 days
        // We set it here as a placeholder, but the mapping will override it
        String expirationDate = Instant.now().plusSeconds(365L * 24 * 60 * 60).toString();
        credential.put("expirationDate", expirationDate);
        credential.put("validThrough", expirationDate); // Also set validThrough for compatibility
        log.debug("Set expiration date: {} (mapping will use <timestamp-in:365d> to generate actual value)", expirationDate);
        
        // 6. Build credential subject
        Map<String, Object> credentialSubject = new LinkedHashMap<>();
        credentialSubject.put("id", subjectDid);
        
        // 7. Map fields from student data using field mappings
        if (template.getFieldMappings() != null) {
            template.getFieldMappings().forEach((credentialField, possibleSourceFields) -> {
                Object value = extractField(studentData, possibleSourceFields);
                if (value != null) {
                    // Handle nested fields (e.g., "address.street" or "street_address" for address object)
                    setNestedField(credentialSubject, credentialField, value);
                } else {
                    log.debug("No value found for field '{}' in student data (tried: {})", 
                        credentialField, possibleSourceFields);
                }
            });
        }
        
        // 8. Group fields with common prefixes into nested objects
        // (e.g., street_address, locality, region → address object)
        groupNestedFields(credentialSubject);
        
        // 9. Calculate age verification fields if birthdate is present
        calculateAgeVerificationFields(credentialSubject);
        
        // 10. Add static fields
        if (template.getStaticFields() != null) {
            credentialSubject.putAll(template.getStaticFields());
        }
        
        credential.put("credentialSubject", credentialSubject);
        
        log.debug("Built credential with {} fields in subject", credentialSubject.size());
        
        return credential;
    }
    
    /**
     * Extracts a field value from student data
     * Tries multiple possible field names in order
     * 
     * @param studentData The student data map
     * @param possibleFields List of possible field names (tried in order)
     * @return The first non-null value found, or null
     */
    private Object extractField(Map<String, Object> studentData, List<String> possibleFields) {
        for (String field : possibleFields) {
            // Try direct access
            Object value = studentData.get(field);
            if (value != null) {
                return value;
            }
            
            // Try nested access (e.g., "address.city")
            if (field.contains(".")) {
                value = extractNestedField(studentData, field);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }
    
    /**
     * Extracts a nested field using dot notation
     * Example: "address.city" -> studentData.get("address").get("city")
     */
    @SuppressWarnings("unchecked")
    private Object extractNestedField(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Sets a field value, creating nested structures if needed
     * Handles both dot notation (address.street) and underscore notation (street_address)
     */
    @SuppressWarnings("unchecked")
    private void setNestedField(Map<String, Object> target, String fieldPath, Object value) {
        // Handle dot notation (e.g., "address.street")
        if (fieldPath.contains(".")) {
            String[] parts = fieldPath.split("\\.", 2);
            String parentKey = parts[0];
            String childPath = parts[1];
            
            // Get or create parent map
            Map<String, Object> parent = (Map<String, Object>) target.computeIfAbsent(
                parentKey, k -> new LinkedHashMap<String, Object>()
            );
            
            // Recursively set child field
            setNestedField(parent, childPath, value);
        } else {
            // Direct field assignment
            target.put(fieldPath, value);
        }
    }
    
    /**
     * Groups fields with underscore notation into nested objects
     * Example: street_address, locality, region, country → address {street_address, locality, region, country}
     * 
     * Special handling for IdentityCredential address fields
     */
    @SuppressWarnings("unchecked")
    private void groupNestedFields(Map<String, Object> credentialSubject) {
        // Check if we have address-related fields that should be grouped
        List<String> addressFields = List.of("street_address", "locality", "region", "country");
        
        boolean hasAddressFields = addressFields.stream()
            .anyMatch(credentialSubject::containsKey);
        
        if (hasAddressFields) {
            Map<String, Object> address = new LinkedHashMap<>();
            
            // Move address fields into nested object
            addressFields.forEach(field -> {
                if (credentialSubject.containsKey(field)) {
                    address.put(field, credentialSubject.remove(field));
                }
            });
            
            // Only add address object if it has fields
            if (!address.isEmpty()) {
                credentialSubject.put("address", address);
                log.debug("Grouped {} address fields into nested 'address' object", address.size());
            }
        }
    }
    
    /**
     * Calculates age verification fields based on birthdate
     * Sets is_over_18, is_over_21, is_over_65 if birthdate is present
     */
    private void calculateAgeVerificationFields(Map<String, Object> credentialSubject) {
        Object birthdateObj = credentialSubject.get("birthdate");
        
        if (birthdateObj != null) {
            try {
                LocalDate birthdate = parseBirthdate(birthdateObj.toString());
                if (birthdate != null) {
                    int age = Period.between(birthdate, LocalDate.now()).getYears();
                    
                    // Only set if not already present in the data
                    credentialSubject.putIfAbsent("is_over_18", age >= 18);
                    credentialSubject.putIfAbsent("is_over_21", age >= 21);
                    credentialSubject.putIfAbsent("is_over_65", age >= 65);
                    
                    log.debug("Calculated age verification fields for age: {}", age);
                }
            } catch (Exception e) {
                log.warn("Failed to calculate age from birthdate '{}': {}", birthdateObj, e.getMessage());
            }
        }
    }
    
    /**
     * Parses a birthdate string in various formats
     */
    private LocalDate parseBirthdate(String birthdateStr) {
        if (birthdateStr == null || birthdateStr.trim().isEmpty()) {
            return null;
        }
        
        // Try common date formats
        List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,           // 1990-01-01
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),  // 1990-01-01
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),  // 01/01/1990
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),  // 01/01/1990
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),  // 01-01-1990
            DateTimeFormatter.ofPattern("yyyy/MM/dd")   // 1990/01/01
        );
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(birthdateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        log.warn("Could not parse birthdate: {}", birthdateStr);
        return null;
    }
    
    /**
     * Evaluates a condition to determine if a credential should be issued
     * Uses SpEL (Spring Expression Language)
     * 
     * @param condition The SpEL expression (e.g., "#studentData['graduationDate'] != null")
     * @param studentData The student data to evaluate against
     * @return true if condition passes or is null, false otherwise
     */
    public boolean evaluateCondition(String condition, Map<String, Object> studentData) {
        if (condition == null || condition.trim().isEmpty()) {
            return true; // No condition = always issue
        }
        
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("studentData", studentData);
            
            Boolean result = expressionParser.parseExpression(condition)
                .getValue(context, Boolean.class);
            
            return result != null && result;
            
        } catch (Exception e) {
            log.warn("Failed to evaluate condition '{}': {}", condition, e.getMessage());
            return false; // Fail safe: don't issue if condition can't be evaluated
        }
    }
    
    /**
     * Builds a default field mapping for backward compatibility
     * This creates the same mapping used by the old CredentialDataBuilder
     */
    public static Map<String, Object> buildDefaultMapping() {
        Map<String, Object> mapping = new HashMap<>();
        
        // Student identity mappings
        mapping.put("id", "$.credentialSubject.id");
        mapping.put("studentId", "$.credentialSubject.schacPersonalUniqueID");
        mapping.put("givenName", "$.credentialSubject.givenName");
        mapping.put("familyName", "$.credentialSubject.familyName");
        mapping.put("email", "$.credentialSubject.email");
        
        // Issuer mappings
        mapping.put("issuer", "$.issuer");
        
        return mapping;
    }
}

