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
 * Converts a generic Object to a UniversityServiceCommonRequest.
 * This converter attempts to handle various source types for flexibility.
 */
@Component
public class ObjectToUniversityServiceCommonRequestConverter
        extends AbstractGenericConverter<Object, UniversityServiceCommonRequest> {

    private final UniversityRegistrationRequestToUniversityServiceCommonRequestConverter regToCommonConverter;

    // Inject the specific converter for reg to common request conversion
    public ObjectToUniversityServiceCommonRequestConverter(
            UniversityRegistrationRequestToUniversityServiceCommonRequestConverter regToCommonConverter) {
        this.regToCommonConverter = regToCommonConverter;
    }

    @Override
    protected UniversityServiceCommonRequest doConvert(@NonNull Object source) {
        // Case 1: Source is already the target type
        if (source instanceof UniversityServiceCommonRequest) {
            return (UniversityServiceCommonRequest) source;
        }

        // Case 2: Source is the other known request type
        if (source instanceof UniversityRegistrationRequest) {
            return regToCommonConverter.doConvert((UniversityRegistrationRequest) source);
        }

        // Case 3: Source is a Map, attempt to extract fields
        if (source instanceof Map) {
            Map<?, ?> sourceMap = (Map<?, ?>) source;
            try {
                String requestId = (String) sourceMap.get("requestId");
                String userName = (String) sourceMap.get("userName");
                String installKey = (String) sourceMap.get("installKey");
                String languageStr = (String) sourceMap.get("language");
                String platformStr = (String) sourceMap.get("platform");
                String application = (String) sourceMap.get("application");
                String versionCode = (String) sourceMap.get("versionCode");

                // Convert string to enum values
                LanguageCode language = languageStr != null ? LanguageCode.valueOf(languageStr) : null;
                PlatformType platform = platformStr != null ? PlatformType.valueOf(platformStr) : null;

                return new UniversityServiceCommonRequest(
                        requestId, userName, installKey, language, platform, application, versionCode,
                        Instant.now(), null, null, null
                );
            } catch (ClassCastException | IllegalArgumentException e) {
                System.err.println("Error casting map values for UniversityServiceCommonRequest conversion: " + e.getMessage());
                // Fall through to default return
            }
        }

        System.err.println("Warning: Cannot convert generic Object of type " + source.getClass().getName() + " to UniversityServiceCommonRequest.");
        return null; // Or throw an IllegalArgumentException
    }
}
