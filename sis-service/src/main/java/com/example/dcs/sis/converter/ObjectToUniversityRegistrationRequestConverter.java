package com.example.dcs.sis.converter;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import com.example.dcs.sis.domain.avro.UniversityRegistrationRequest;
import com.example.dcs.sis.domain.avro.UniversityServiceCommonRequest;
import com.example.dcs.sis.domain.avro.LanguageCode;
import com.example.dcs.sis.domain.avro.PlatformType;
import java.time.Instant;
import java.util.Map;

/**
 * Converts a generic Object to a UniversityRegistrationRequest.
 * This converter attempts to handle various source types for flexibility.
 */
@Component
public class ObjectToUniversityRegistrationRequestConverter
        extends AbstractGenericConverter<Object, UniversityRegistrationRequest> {

    private final UniversityServiceCommonRequestToUniversityRegistrationRequestConverter commonToRegConverter;

    // Inject the specific converter for common to reg request conversion
    public ObjectToUniversityRegistrationRequestConverter(
            UniversityServiceCommonRequestToUniversityRegistrationRequestConverter commonToRegConverter) {
        this.commonToRegConverter = commonToRegConverter;
    }

    @Override
    protected UniversityRegistrationRequest doConvert(@NonNull Object source) {
        // Case 1: Source is already the target type
        if (source instanceof UniversityRegistrationRequest) {
            return (UniversityRegistrationRequest) source;
        }

        // Case 2: Source is the other known request type
        if (source instanceof UniversityServiceCommonRequest) {
            return commonToRegConverter.doConvert((UniversityServiceCommonRequest) source);
        }

        // Case 3: Source is a Map, attempt to extract fields
        if (source instanceof Map) {
            Map<?, ?> sourceMap = (Map<?, ?>) source;
            try {
                String requestId = (String) sourceMap.get("requestId");
                String userName = (String) sourceMap.get("userName");
                String languageStr = (String) sourceMap.get("language");
                String platformStr = (String) sourceMap.get("platform");
                String password = (String) sourceMap.get("password");
                String application = (String) sourceMap.get("application");
                String versionCode = null;
                Object versionCodeObj = sourceMap.get("versionCode");
                if (versionCodeObj != null) {
                    versionCode = String.valueOf(versionCodeObj); // Convert anything to String
                }

                // Convert string to enum values
                LanguageCode language = languageStr != null ? LanguageCode.valueOf(languageStr) : null;
                PlatformType platform = platformStr != null ? PlatformType.valueOf(platformStr) : null;

                return new UniversityRegistrationRequest(
                        requestId, userName, language, platform, password, application, versionCode,
                        Instant.now(), null, null, null, null
                );
            } catch (ClassCastException | IllegalArgumentException e) {
                System.err.println("Error casting map values for UniversityRegistrationRequest conversion: " + e.getMessage());
                // Fall through to default return
            }
        }

        System.err.println("Warning: Cannot convert generic Object of type " + source.getClass().getName() + " to UniversityRegistrationRequest.");
        return null; // Or throw an IllegalArgumentException
    }
}
