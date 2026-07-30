package pt.ulusofona.digital.wallet.converter;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import pt.ulusofona.digital.wallet.domain.avro.UniversityRegistrationRequest;
import pt.ulusofona.digital.wallet.domain.avro.UniversityServiceCommonRequest;

/**
 * Converts a UniversityServiceCommonRequest to a UniversityRegistrationRequest.
 */
@Component
public class UniversityServiceCommonRequestToUniversityRegistrationRequestConverter
        extends AbstractGenericConverter<UniversityServiceCommonRequest, UniversityRegistrationRequest> {

    @Override
    protected UniversityRegistrationRequest doConvert(@NonNull UniversityServiceCommonRequest source) {
        // 'password' is not present in UniversityServiceCommonRequest.
        // We'll set a default or null value.
        String password = "DEFAULT_REG_PASSWORD"; // Or null, depending on your requirements.

        return new UniversityRegistrationRequest(
                source.getRequestId(),
                source.getUserName(),
                source.getLanguage(),
                source.getPlatform(),
                password,
                source.getApplication(),
                source.getVersionCode(),
                source.getRequestTimestamp(),
                source.getCorrelationId(),
                null, // installKey - not mapping back to registration
                null, // deviceInfo
                null  // registrationMetadata
        );
    }
}