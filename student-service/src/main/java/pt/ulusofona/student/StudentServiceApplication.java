package pt.ulusofona.student;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Student Service - Entry Point for Student Requests
 * 
 * Purpose: Simplest possible orchestrator
 * - Receives student login (minimal data)
 * - Publishes to Kafka
 * - Returns correlationId
 * 
 * NO authentication, NO business logic, NO external APIs!
 */
@SpringBootApplication
@EnableKafka
@EnableFeignClients
public class StudentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentServiceApplication.class, args);
    }
}


