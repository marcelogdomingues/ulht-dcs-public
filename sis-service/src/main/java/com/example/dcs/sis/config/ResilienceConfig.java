package com.example.dcs.sis.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker and retry configuration for external service calls.
 * Provides resilience patterns for handling WaltID API failures.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Creates a circuit breaker registry with custom configuration for WaltID services
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% failure rate threshold
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds before trying again
                .permittedNumberOfCallsInHalfOpenState(5) // Allow 5 calls in half-open state
                .slidingWindowSize(10) // Consider last 10 calls for failure rate calculation
                .minimumNumberOfCalls(5) // Minimum calls before calculating failure rate
                .recordExceptions(
                        java.net.ConnectException.class,
                        java.net.SocketTimeoutException.class,
                        java.io.IOException.class,
                        org.springframework.web.client.ResourceAccessException.class,
                        feign.FeignException.class
                )
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Creates a retry registry with exponential backoff for WaltID services
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(5) // Maximum 5 retry attempts
                .waitDuration(Duration.ofSeconds(1)) // Initial wait of 1 second
                .retryExceptions(
                        java.net.ConnectException.class,
                        java.net.SocketTimeoutException.class,
                        java.io.IOException.class,
                        org.springframework.web.client.ResourceAccessException.class,
                        feign.FeignException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        NullPointerException.class
                )
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * Creates a circuit breaker specifically for WaltID wallet operations
     */
    @Bean
    public CircuitBreaker waltidWalletCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("waltid-wallet");
    }

    /**
     * Creates a circuit breaker specifically for WaltID issuer operations
     */
    @Bean
    public CircuitBreaker waltidIssuerCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("waltid-issuer");
    }

    /**
     * Creates a retry instance for WaltID wallet operations
     */
    @Bean
    public Retry waltidWalletRetry(RetryRegistry registry) {
        return registry.retry("waltid-wallet");
    }

    /**
     * Creates a retry instance for WaltID issuer operations
     */
    @Bean
    public Retry waltidIssuerRetry(RetryRegistry registry) {
        return registry.retry("waltid-issuer");
    }
} 