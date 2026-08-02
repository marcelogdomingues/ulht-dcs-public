package com.example.dcs.student.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration scoped to {@link FulfilmentClient}.
 * <p>
 * Adds the shared {@code apikey} header to every request so that the internal
 * call to the fulfilment-service passes its API-key authentication filter.
 * This configuration must NOT be applied to external clients.
 * <p>
 * Note: this class is intentionally NOT annotated with {@code @Configuration}
 * so it is only used by the client that references it via
 * {@code @FeignClient(configuration = FulfilmentClientConfig.class)} and is not
 * picked up by component scanning as a global Feign configuration.
 */
public class FulfilmentClientConfig {

    @Bean
    public RequestInterceptor fulfilmentApiKeyInterceptor(
            @Value("${app.security.api-key}") String apiKey) {
        return template -> template.header("apikey", apiKey);
    }
}
