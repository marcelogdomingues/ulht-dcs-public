package pt.ulusofona.digital.wallet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pt.ulusofona.digital.wallet.clients.LusofonaClient;

@Configuration
@EnableFeignClients(clients = {LusofonaClient.class})
public class AppConfiguration implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:8000,http://localhost:3000}")
    private String[] allowedOrigins;

    /**
     * Spring Boot 4 auto-configures a Jackson 3 (tools.jackson) ObjectMapper for HTTP;
     * this app's code uses the Jackson 2 (com.fasterxml) ObjectMapper, which is no longer
     * auto-configured, so provide it explicitly.
     */
    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("apikey", "Content-Type", "Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
