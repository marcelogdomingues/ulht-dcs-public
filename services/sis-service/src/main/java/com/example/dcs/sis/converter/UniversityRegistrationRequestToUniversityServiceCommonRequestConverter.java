package com.example.dcs.sis.converter;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import com.example.dcs.sis.domain.avro.UniversityRegistrationRequest;
import com.example.dcs.sis.domain.avro.UniversityServiceCommonRequest;

@Component
public class UniversityRegistrationRequestToUniversityServiceCommonRequestConverter
        extends AbstractGenericConverter<UniversityRegistrationRequest, UniversityServiceCommonRequest> {

    @Override
    protected UniversityServiceCommonRequest doConvert(@NonNull UniversityRegistrationRequest source) {
        // 'installKey' is not present in UniversityRegistrationRequest.
        // We'll generate a simple one based on userName or set a default.
        String installKey = source.getInstallKey();
        if (installKey == null) {
            installKey = source.getUserName() != null ? source.getUserName() + "_AUTO_KEY" : "DEFAULT_INSTALL_KEY";
        }

        return new UniversityServiceCommonRequest(
                source.getRequestId(),
                source.getUserName(),
                installKey,
                source.getLanguage(),
                source.getPlatform(),
                source.getApplication(),
                source.getVersionCode(),
                source.getRequestTimestamp(),
                source.getCorrelationId(),
                null, // clientInfo
                null  // requestMetadata
        );
    }
}
