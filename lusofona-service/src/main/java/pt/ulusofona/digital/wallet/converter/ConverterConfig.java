package pt.ulusofona.digital.wallet.converter;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring configuration to register custom converters.
 * This ensures that Spring's ConversionService can find and use our converters.
 */
@Configuration
public class ConverterConfig implements WebMvcConfigurer {

    private final UniversityRegistrationRequestToUniversityServiceCommonRequestConverter regToCommonConverter;
    private final UniversityServiceCommonRequestToUniversityRegistrationRequestConverter commonToRegConverter;
    private final ObjectToUniversityRegistrationRequestConverter objectToRegConverter; // New field
    private final ObjectToUniversityServiceCommonRequestConverter objectToCommonConverter; // New field

    // Spring will automatically inject our converter beans
    public ConverterConfig(
            UniversityRegistrationRequestToUniversityServiceCommonRequestConverter regToCommonConverter,
            UniversityServiceCommonRequestToUniversityRegistrationRequestConverter commonToRegConverter,
            ObjectToUniversityRegistrationRequestConverter objectToRegConverter, // New parameter
            ObjectToUniversityServiceCommonRequestConverter objectToCommonConverter) { // New parameter
        this.regToCommonConverter = regToCommonConverter;
        this.commonToRegConverter = commonToRegConverter;
        this.objectToRegConverter = objectToRegConverter; // Assign new field
        this.objectToCommonConverter = objectToCommonConverter; // Assign new field
    }

    /**
     * Adds our custom converters to Spring's FormatterRegistry, making them available
     * to the ConversionService.
     *
     * @param registry The FormatterRegistry to which converters are added.
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(regToCommonConverter);
        registry.addConverter(commonToRegConverter);
        registry.addConverter(objectToRegConverter); // Register new converter
        registry.addConverter(objectToCommonConverter); // Register new converter
    }
}