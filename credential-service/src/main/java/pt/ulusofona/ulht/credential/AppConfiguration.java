package pt.ulusofona.ulht.credential;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pt.ulusofona.ulht.credential.client.WaltidWalletClient;
import pt.ulusofona.ulht.credential.client.WaltidIssuerClient;
import pt.ulusofona.ulht.credential.client.WaltidVerifierClient;
import pt.ulusofona.ulht.credential.config.PlainTextAwareDecoder;
import pt.ulusofona.ulht.credential.config.PlainTextAwareEncoder;

@Configuration
@EnableFeignClients(clients = {WaltidIssuerClient.class, WaltidWalletClient.class, WaltidVerifierClient.class})
@EnableKafka
@EnableScheduling
public class AppConfiguration implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:8000,http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("apikey", "Content-Type", "Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Bean
    @Primary
    public Encoder feignEncoder() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Disable pretty printing to avoid extra whitespace
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        // Register JSR310 module to handle java.time types
        objectMapper.registerModule(new JavaTimeModule());
        // Wrap Jackson encoder with plain text aware encoder
        return new PlainTextAwareEncoder(new JacksonEncoder(objectMapper));
    }

    @Bean
    @Primary
    public Decoder feignDecoder() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Register JSR310 module to handle java.time types
        objectMapper.registerModule(new JavaTimeModule());
        return new PlainTextAwareDecoder(new JacksonDecoder(objectMapper));
    }

    /**
     * Spring Boot 4 auto-configures a Jackson 3 (tools.jackson) ObjectMapper for HTTP.
     * This app's internal code uses the Jackson 2 (com.fasterxml) ObjectMapper, which is
     * no longer auto-configured, so provide it explicitly.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
