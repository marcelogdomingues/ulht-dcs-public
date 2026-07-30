package pt.ulusofona.student.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * Feign client to proxy requests to Fulfilment Service
 * This allows Student Service to provide a unified API
 */
@FeignClient(
    name = "fulfilment-service",
    url = "${services.fulfilment.url}",
    configuration = FulfilmentClientConfig.class
)
public interface FulfilmentClient {

    /**
     * Get workflow status from Fulfilment Service
     */
    @GetMapping("/fulfilment/status/{correlationId}")
    ResponseEntity<Map<String, Object>> getStatus(@PathVariable String correlationId);

    /**
     * Get workflow progress from Fulfilment Service
     */
    @GetMapping("/fulfilment/progress/{correlationId}")
    ResponseEntity<Map<String, Object>> getProgress(@PathVariable String correlationId);

    /**
     * Get final result/credentials from Fulfilment Service
     */
    @GetMapping("/fulfilment/result/{correlationId}")
    ResponseEntity<Map<String, Object>> getResult(@PathVariable String correlationId);

    /**
     * Initialize a workflow in Fulfilment Service
     * This ensures the workflow is tracked immediately before Kafka events are consumed
     */
    @PostMapping("/fulfilment/initialize/{correlationId}")
    ResponseEntity<Map<String, String>> initializeWorkflow(@PathVariable String correlationId);
}


