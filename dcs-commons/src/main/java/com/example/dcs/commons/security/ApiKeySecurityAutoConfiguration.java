package com.example.dcs.commons.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Shared API-key based security auto-configuration for the DCS services.
 *
 * <p>Stateless: every request must present a valid {@code apikey} header, except
 * the public endpoints (actuator health/info/prometheus + swagger).
 *
 * <p>This is registered via Spring Boot auto-configuration
 * ({@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}),
 * so a service only needs the {@code dcs-commons} dependency on its classpath —
 * no per-service {@code @Import} is required, and it works regardless of the
 * service's own base package.
 *
 * <p>Every bean is guarded with {@link ConditionalOnMissingBean} so an individual
 * service can still fully override the behaviour by declaring its own
 * {@code SecurityFilterChain} / {@code CorsConfigurationSource}.
 *
 * <h2>Dual auth (api-key OR OAuth2 JWT)</h2>
 *
 * <p>The api-key path above is always active. In ADDITION, when an OAuth2 issuer
 * is configured (property {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}
 * or {@code ...jwt.jwk-set-uri}), Spring Boot's
 * {@code OAuth2ResourceServerAutoConfiguration} contributes a {@link JwtDecoder}
 * bean. When that bean is present, this filter chain additionally wires
 * {@code http.oauth2ResourceServer(...jwt...)}, so a valid
 * {@code Authorization: Bearer <jwt>} ALSO authenticates the request.
 *
 * <p>A request is therefore authenticated if it presents EITHER a valid
 * {@code apikey} header OR a valid Bearer JWT; with neither it still gets
 * {@code 401 Unauthorized}. Crucially, when NO issuer-uri is configured there is
 * no {@link JwtDecoder} bean, the oauth2 leg is never wired, and the chain
 * behaves byte-for-byte as it did before — api-key only, with no attempt to reach
 * any (non-existent) identity provider. This keeps the demo and existing runs
 * unchanged.
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code app.security.api-key} — the shared secret required in the {@code apikey} header.</li>
 *   <li>{@code app.cors.allowed-origins} — comma separated list of allowed origins
 *       (defaults to {@code http://localhost:8000,http://localhost:3000}).</li>
 *   <li>{@code spring.security.oauth2.resourceserver.jwt.issuer-uri} — OPTIONAL;
 *       when set, enables the additional Bearer-JWT auth leg (OIDC / Keycloak).</li>
 * </ul>
 */
@AutoConfiguration(before = {
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@ConditionalOnClass({SecurityFilterChain.class, HttpSecurity.class})
@EnableWebSecurity
public class ApiKeySecurityAutoConfiguration {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // OpenAPI spec. Services override springdoc.api-docs.path to
            // "/api-docs", so permit both that and the springdoc default,
            // otherwise Swagger UI loads but gets 401 fetching its spec
            // (and /api-docs/swagger-config, which the UI needs on startup).
            "/api-docs/**",
            "/v3/api-docs/**"
    };

    @Value("${app.security.api-key}")
    private String apiKey;

    @Value("${app.cors.allowed-origins:http://localhost:8000,http://localhost:3000}")
    private String[] allowedOrigins;

    /**
     * The single shared security chain for every DCS service.
     *
     * @param http        the {@link HttpSecurity} builder
     * @param jwtDecoder  provider for an OAuth2 {@link JwtDecoder}. This bean is
     *                    only contributed by Spring Boot's
     *                    {@code OAuth2ResourceServerAutoConfiguration} when an
     *                    {@code issuer-uri}/{@code jwk-set-uri} is configured. When
     *                    absent, the Bearer-JWT auth leg is NOT wired and the chain
     *                    is api-key only (identical to prior behaviour).
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectProvider<JwtDecoder> jwtDecoder) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new ApiKeyAuthFilter(apiKey),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));

        // Dual auth: ADDITIONALLY accept a valid `Authorization: Bearer <jwt>`,
        // but only when an OAuth2 issuer is configured (i.e. a JwtDecoder exists).
        // The ApiKeyAuthFilter still runs first, so a valid apikey authenticates
        // as before regardless of any Bearer token. With no issuer configured,
        // this block is a no-op and the chain is byte-for-byte api-key only.
        JwtDecoder decoder = jwtDecoder.getIfAvailable();
        if (decoder != null) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(decoder)));
        }

        return http.build();
    }

    /**
     * CORS source used by Spring Security. Keeping this in sync with the MVC CORS
     * config lets Security auto-permit preflight (OPTIONS) requests.
     */
    @Bean
    @ConditionalOnMissingBean(CorsConfigurationSource.class)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("apikey", "Content-Type", "Authorization"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
