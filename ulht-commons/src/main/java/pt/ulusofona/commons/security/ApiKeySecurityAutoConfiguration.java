package pt.ulusofona.commons.security;

import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Shared API-key based security auto-configuration for the ULHT services.
 *
 * <p>Stateless: every request must present a valid {@code apikey} header, except
 * the public endpoints (actuator health/info/prometheus + swagger).
 *
 * <p>This is registered via Spring Boot auto-configuration
 * ({@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}),
 * so a service only needs the {@code ulht-commons} dependency on its classpath —
 * no per-service {@code @Import} is required, and it works regardless of the
 * service's own base package.
 *
 * <p>Every bean is guarded with {@link ConditionalOnMissingBean} so an individual
 * service can still fully override the behaviour by declaring its own
 * {@code SecurityFilterChain} / {@code CorsConfigurationSource}.
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code app.security.api-key} — the shared secret required in the {@code apikey} header.</li>
 *   <li>{@code app.cors.allowed-origins} — comma separated list of allowed origins
 *       (defaults to {@code http://localhost:8000,http://localhost:3000}).</li>
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
            "/v3/api-docs/**"
    };

    @Value("${app.security.api-key}")
    private String apiKey;

    @Value("${app.cors.allowed-origins:http://localhost:8000,http://localhost:3000}")
    private String[] allowedOrigins;

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
