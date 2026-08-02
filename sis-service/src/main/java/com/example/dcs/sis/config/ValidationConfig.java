package com.example.dcs.sis.config;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Configuration class for Bean Validation setup.
 * Provides centralized validation configuration and custom validators.
 */
@Configuration
public class ValidationConfig {
    
    /**
     * Creates a LocalValidatorFactoryBean for Spring integration.
     * This enables automatic validation of method parameters and return values.
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setProviderClass(org.hibernate.validator.HibernateValidator.class);
        return factory;
    }
    
    /**
     * Creates a standard Validator instance for programmatic validation.
     * Useful for custom validation scenarios and testing.
     */
    @Bean
    public Validator validatorFactory() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }
}

