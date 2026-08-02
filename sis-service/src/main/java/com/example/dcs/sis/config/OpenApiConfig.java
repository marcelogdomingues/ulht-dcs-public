package com.example.dcs.sis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DCS Digital Wallet API")
                        .description("""
                            # DCS Digital Wallet API
                            
                            This API provides comprehensive integration between DCS (Example University de Humanidades e Tecnologias) 
                            and WaltID digital wallet platform. It enables secure management of student academic data, 
                            digital credentials, and seamless integration with the university's information systems.
                            
                            ## Key Features
                            
                            - **Student Authentication**: Secure login and registration for students
                            - **Academic Data Retrieval**: Access to grades, enrollments, schedules, and evaluations
                            - **Digital Credentials**: Integration with WaltID for digital credential management
                            - **Real-time Updates**: Kafka-based messaging for real-time data synchronization
                            
                            ## Authentication
                            
                            This API uses student credentials for authentication. All requests require valid student 
                            credentials and platform information.
                            
                            ## Rate Limiting
                            
                            API requests are subject to rate limiting to ensure fair usage and system stability.
                            
                            ## Support
                            
                            For technical support or questions about this API, please contact the DCS Development Team.
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DCS Development Team")
                                .email("dev@usis.pt")
                                .url("https://www.usis.pt"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8085")
                                .description("Development server - for local development and testing"),
                        new Server()
                                .url("https://api.usis.pt")
                                .description("Production server - live environment"),
                        new Server()
                                .url("https://staging-api.usis.pt")
                                .description("Staging server - pre-production testing")
                ))
                .components(new Components()
                        .addSecuritySchemes("studentAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Student credentials authentication")
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList("studentAuth"))
                .externalDocs(new ExternalDocumentation()
                        .description("DCS Digital Wallet Documentation")
                        .url("https://docs.usis.pt/digital-wallet"))
                .tags(List.of(
                        new Tag()
                                .name("Student Services")
                                .description("Endpoints for managing student data and academic information")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Student Services Documentation")
                                        .url("https://docs.usis.pt/student-services")),
                        new Tag()
                                .name("Authentication")
                                .description("Student authentication and registration endpoints")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Authentication Documentation")
                                        .url("https://docs.usis.pt/authentication")),
                        new Tag()
                                .name("Kafka Testing")
                                .description("Endpoints for testing Kafka message production")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Kafka Integration Documentation")
                                        .url("https://docs.usis.pt/kafka-integration")),
                        new Tag()
                                .name("System")
                                .description("System health and status endpoints")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("System Documentation")
                                        .url("https://docs.usis.pt/system"))
                ));
    }
} 