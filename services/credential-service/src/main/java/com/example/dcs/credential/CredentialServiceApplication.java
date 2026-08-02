package com.example.dcs.credential;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.example.dcs.credential.config")
public class CredentialServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CredentialServiceApplication.class, args);
    }

}
