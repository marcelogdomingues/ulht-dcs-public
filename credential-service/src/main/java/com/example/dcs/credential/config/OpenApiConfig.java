package com.example.dcs.credential.config;

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
                        .title("DCS Credential Service API")
                        .description("""
                            # DCS Credential Service API
                            
                            This API provides comprehensive digital credential management and wallet integration 
                            services for DCS (Example University de Humanidades e Tecnologias). It enables 
                            secure issuance, management, and verification of digital credentials through 
                            integration with the WaltID platform.
                            
                            ## Key Features
                            
                            - **Digital Credential Issuance**: Secure issuance of verifiable credentials
                            - **Wallet Management**: Complete wallet lifecycle management
                            - **User Authentication**: Secure user registration and authentication
                            - **Flow Orchestration**: Intelligent workflow management for complex operations
                            - **Real-time Processing**: Kafka-based event-driven architecture
                            
                            ## Authentication
                            
                            This API uses session-based authentication with cookies. Users must first 
                            authenticate through the login endpoint to receive a session cookie.
                            
                            ## Flow Management
                            
                            The service uses intelligent flow orchestration to handle complex multi-step 
                            operations. Each flow is identified by a correlation ID that can be used to 
                            track progress and retrieve results.
                            
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
                                .url("http://localhost:8086")
                                .description("Development server - for local development and testing"),
                        new Server()
                                .url("https://api.usis.pt/credential")
                                .description("Production server - live environment"),
                        new Server()
                                .url("https://staging-api.usis.pt/credential")
                                .description("Staging server - pre-production testing")
                ))
                .components(new Components()
                        .addSecuritySchemes("sessionAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("login")
                                .description("Session cookie for authentication")))
                .addSecurityItem(new SecurityRequirement().addList("sessionAuth"))
                .externalDocs(new ExternalDocumentation()
                        .description("DCS Credential Service Documentation")
                        .url("https://docs.usis.pt/credential-service"))
                .tags(List.of(
                        new Tag()
                                .name("Wallet Authentication")
                                .description("User authentication and registration for wallet access")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Authentication Documentation")
                                        .url("https://docs.usis.pt/authentication")),
                        new Tag()
                                .name("Flow Management")
                                .description("Intelligent workflow orchestration for complex operations")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Flow Management Documentation")
                                        .url("https://docs.usis.pt/flow-management")),
                        new Tag()
                                .name("Credential Issuance")
                                .description("Digital credential creation and issuance")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Credential Documentation")
                                        .url("https://docs.usis.pt/credentials")),
                        new Tag()
                                .name("System")
                                .description("System health and monitoring endpoints")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("System Documentation")
                                        .url("https://docs.usis.pt/system"))
                ));
    }
} 