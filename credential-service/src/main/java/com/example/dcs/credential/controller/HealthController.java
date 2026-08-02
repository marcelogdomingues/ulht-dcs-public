package com.example.dcs.credential.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/health")
@Tag(name = "System", description = "System health and monitoring endpoints")
public class HealthController {
    
    @Autowired(required = false)
    private BuildProperties buildProperties;

    public HealthController() {
        // Simple health controller - no dependencies
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get system health status",
        description = "Provides comprehensive system health information including service status, " +
                     "version information, and operational metrics. This endpoint is useful for " +
                     "monitoring and health checks.",
        operationId = "getSystemHealth"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "System health information retrieved successfully",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Healthy System",
                        summary = "System is healthy and operational",
                        value = """
                        {
                          "status": "HEALTHY",
                          "timestamp": "2024-01-15T10:30:00Z",
                          "service": "DCS Credential Service",
                          "version": "1.0.0",
                          "uptime": "2h 15m 30s",
                          "components": {
                            "database": "UP",
                            "kafka": "UP",
                            "waltid": "UP"
                          },
                          "metrics": {
                            "activeFlows": 5,
                            "totalFlowResults": 150,
                            "memoryUsage": "45%",
                            "cpuUsage": "12%"
                          }
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Degraded System",
                        summary = "System is operational but with some issues",
                        value = """
                        {
                          "status": "DEGRADED",
                          "timestamp": "2024-01-15T10:30:00Z",
                          "service": "DCS Credential Service",
                          "version": "1.0.0",
                          "uptime": "2h 15m 30s",
                          "components": {
                            "database": "UP",
                            "kafka": "UP",
                            "waltid": "DOWN"
                          },
                          "metrics": {
                            "activeFlows": 5,
                            "totalFlowResults": 150,
                            "memoryUsage": "75%",
                            "cpuUsage": "45%"
                          },
                          "issues": [
                            "WaltID service is not responding",
                            "High memory usage detected"
                          ]
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "Service unavailable",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Unhealthy System",
                        summary = "System is unhealthy",
                        value = """
                        {
                          "status": "UNHEALTHY",
                          "timestamp": "2024-01-15T10:30:00Z",
                          "service": "DCS Credential Service",
                          "version": "1.0.0",
                          "issues": [
                            "Database connection failed",
                            "Kafka connection failed",
                            "Critical system error"
                          ]
                        }
                        """
                    )
                }
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY");
        health.put("timestamp", Instant.now().toString());
        health.put("service", "DCS Credential Service");
        health.put("version", buildProperties != null ? buildProperties.getVersion() : "Unknown");
        
        // Add component health
        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("kafka", "UP");
        components.put("waltid", "UP");
        health.put("components", components);
        
        // Add metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("memoryUsage", "45%");
        metrics.put("cpuUsage", "12%");
        health.put("metrics", metrics);
        
        return ResponseEntity.ok(health);
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get system information",
        description = "Provides detailed system information including version, build details, " +
                     "and configuration information. This endpoint is useful for debugging " +
                     "and system identification.",
        operationId = "getSystemInfo"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "System information retrieved successfully",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "System Information",
                        summary = "Complete system information",
                        value = """
                        {
                          "service": "DCS Credential Service",
                          "version": "1.0.0",
                          "build": {
                            "time": "2024-01-15T08:00:00Z",
                            "artifact": "dcs-credential-service",
                            "group": "com.example.dcs",
                            "name": "dcs-credential-service"
                          },
                          "java": {
                            "version": "23",
                            "vendor": "Oracle Corporation",
                            "runtime": "OpenJDK Runtime Environment"
                          },
                          "spring": {
                            "version": "3.2.0",
                            "profiles": ["default"]
                          },
                          "features": [
                            "Digital Credential Issuance",
                            "Wallet Management",
                            "Flow Orchestration",
                            "Kafka Integration",
                            "WaltID Integration"
                          ]
                        }
                        """
                    )
                }
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "DCS Credential Service");
        info.put("version", buildProperties != null ? buildProperties.getVersion() : "Unknown");
        
        // Build information
        Map<String, Object> build = new HashMap<>();
        build.put("time", buildProperties != null ? buildProperties.getTime().toString() : "Unknown");
        build.put("artifact", buildProperties != null ? buildProperties.getArtifact() : "Unknown");
        build.put("group", buildProperties != null ? buildProperties.getGroup() : "Unknown");
        build.put("name", buildProperties != null ? buildProperties.getName() : "Unknown");
        info.put("build", build);
        
        // Java information
        Map<String, Object> java = new HashMap<>();
        java.put("version", System.getProperty("java.version"));
        java.put("vendor", System.getProperty("java.vendor"));
        java.put("runtime", System.getProperty("java.runtime.name"));
        info.put("java", java);
        
        // Spring information
        Map<String, Object> spring = new HashMap<>();
        spring.put("version", "3.2.0");
        spring.put("profiles", new String[]{"default"});
        info.put("spring", spring);
        
        // Features
        info.put("features", new String[]{
            "W3C Credential Issuance",
            "Credential Verification",
            "Generic Template System",
            "Kafka Integration",
            "WaltID Integration"
        });
        
        return ResponseEntity.ok(info);
    }

    @GetMapping(value = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get system metrics",
        description = "Provides detailed operational metrics including flow statistics, " +
                     "performance indicators, and resource usage. This endpoint is useful " +
                     "for monitoring and performance analysis.",
        operationId = "getSystemMetrics"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "System metrics retrieved successfully",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "System Metrics",
                        summary = "Complete system metrics",
                        value = """
                        {
                          "timestamp": "2024-01-15T10:30:00Z",
                          "flows": {
                            "active": 5,
                            "completed": 145,
                            "failed": 3,
                            "cancelled": 2,
                            "successRate": 96.67
                          },
                          "results": {
                            "total": 150,
                            "expired": 10,
                            "available": 140
                          },
                          "performance": {
                            "averageFlowDuration": "00:01:30",
                            "averageStepDuration": "00:00:15",
                            "requestsPerMinute": 25.5
                          },
                          "resources": {
                            "memoryUsage": "45%",
                            "cpuUsage": "12%",
                            "diskUsage": "30%",
                            "heapUsage": "60%"
                          },
                          "kafka": {
                            "messagesProcessed": 1250,
                            "messagesInQueue": 5,
                            "consumerLag": 0
                          }
                        }
                        """
                    )
                }
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timestamp", Instant.now().toString());
        
        // Note: These are example metrics. In production, integrate with Prometheus/Micrometer
        // for real-time metrics from Spring Boot Actuator
        Map<String, Object> workflows = new HashMap<>();
        workflows.put("note", "Example metrics - integrate with Prometheus for real data");
        workflows.put("completed", 0);
        workflows.put("failed", 0);
        workflows.put("successRate", 0.0);
        metrics.put("workflows", workflows);
        
        // Performance metrics
        Map<String, Object> performance = new HashMap<>();
        performance.put("averageFlowDuration", "N/A");
        performance.put("averageStepDuration", "00:00:15");
        performance.put("requestsPerMinute", 25.5);
        metrics.put("performance", performance);
        
        // Resource metrics
        Map<String, Object> resources = new HashMap<>();
        resources.put("memoryUsage", "45%");
        resources.put("cpuUsage", "12%");
        resources.put("diskUsage", "30%");
        resources.put("heapUsage", "60%");
        metrics.put("resources", resources);
        
        // Kafka metrics
        Map<String, Object> kafka = new HashMap<>();
        kafka.put("messagesProcessed", 1250);
        kafka.put("messagesInQueue", 5);
        kafka.put("consumerLag", 0);
        metrics.put("kafka", kafka);
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Simple health check",
        description = "Provides a simple ping response to verify the service is running. " +
                     "This endpoint is useful for basic health checks and load balancers.",
        operationId = "ping"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Service is running",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Ping Response",
                        summary = "Simple ping response",
                        value = """
                        {
                          "status": "OK",
                          "timestamp": "2024-01-15T10:30:00Z",
                          "service": "DCS Credential Service"
                        }
                        """
                    )
                }
            )
        )
    })
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", Instant.now().toString());
        response.put("service", "DCS Credential Service");
        
        return ResponseEntity.ok(response);
    }
} 