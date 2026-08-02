package com.example.dcs.fulfilment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * DCS Fulfilment Service Application
 * 
 * A dedicated microservice for handling workflow fulfilment and client notifications.
 * This service manages the "end" of credential workflows by providing real-time
 * progress updates and final results to clients via Server-Sent Events (SSE).
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
public class FulfilmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FulfilmentServiceApplication.class, args);
    }
}
