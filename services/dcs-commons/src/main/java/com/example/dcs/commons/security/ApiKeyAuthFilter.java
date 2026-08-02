package com.example.dcs.commons.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Filter that authenticates requests using a shared API key supplied in the
 * {@code apikey} header. The provided key is compared to the configured
 * expected key using a constant-time comparison to avoid timing attacks.
 *
 * <p>This is the single canonical implementation shared by all DCS services
 * via the {@code dcs-commons} module.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "apikey";

    private final byte[] expectedKeyBytes;

    public ApiKeyAuthFilter(String expectedApiKey) {
        this.expectedKeyBytes = expectedApiKey == null
                ? new byte[0]
                : expectedApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(API_KEY_HEADER);

        if (providedKey != null && isValid(providedKey)) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "service",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValid(String providedKey) {
        byte[] providedBytes = providedKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedKeyBytes, providedBytes);
    }
}
