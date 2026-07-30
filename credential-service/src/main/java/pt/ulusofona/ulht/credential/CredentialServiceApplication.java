package pt.ulusofona.ulht.credential;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("pt.ulusofona.ulht.credential.config")
public class CredentialServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CredentialServiceApplication.class, args);
    }

}
