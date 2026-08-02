package com.example.dcs.fulfilment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.example.dcs.fulfilment.domain.WorkflowStatus;
import com.example.dcs.fulfilment.service.FulfilmentService;

import java.util.Map;

/**
 * Controller for workflow fulfilment and tracking endpoints.
 * Provides Server-Sent Events (SSE) for real-time workflow progress updates.
 */
@Slf4j
@RestController
@RequestMapping("/fulfilment")
@RequiredArgsConstructor
@Tag(name = "Fulfilment Service", description = "Workflow fulfilment and progress tracking")
public class FulfilmentController {
    
    private final FulfilmentService fulfilmentService;
    
    /**
     * Subscribe to real-time workflow progress updates via Server-Sent Events
     * 
     * @param correlationId The workflow correlation ID to track
     * @return SseEmitter for real-time updates
     */
    @GetMapping(value = "/track/{correlationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Track workflow progress",
        description = "Subscribe to real-time workflow progress updates using Server-Sent Events (SSE). " +
                     "This endpoint provides live updates on the status of credential workflows including " +
                     "progress percentages, status messages, and final results or errors."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully connected to workflow tracking"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Workflow not found or correlation ID invalid"
        )
    })
    public SseEmitter trackWorkflow(
            @Parameter(description = "Unique correlation ID for the workflow to track", required = true)
            @PathVariable String correlationId) {
        
        log.info("Creating SSE connection for workflow tracking: {}", correlationId);
        
        return fulfilmentService.createConnection(correlationId);
    }
    
    /**
     * Get the current status of a workflow
     * 
     * @param correlationId The workflow correlation ID
     * @return Current workflow status
     */
    @GetMapping("/status/{correlationId}")
    @Operation(
        summary = "Get workflow status",
        description = "Retrieve the current status of a workflow by its correlation ID. " +
                     "This is a REST endpoint alternative to SSE for polling-based status checking. " +
                     "Returns a PROCESSING status if the workflow hasn't been tracked yet (Kafka events are asynchronous)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Workflow status retrieved successfully"
        )
    })
    public ResponseEntity<WorkflowStatus> getWorkflowStatus(
            @Parameter(description = "Unique correlation ID for the workflow", required = true)
            @PathVariable String correlationId) {
        
        log.debug("Getting status for workflow: {}", correlationId);
        
        WorkflowStatus status = fulfilmentService.getWorkflowStatus(correlationId);
        if (status == null) {
            // Workflow not tracked yet - Kafka events are asynchronous
            // Return a default PROCESSING status instead of 404
            // This allows the app to continue polling until the workflow is tracked
            log.debug("Workflow not yet tracked: {} - returning default PROCESSING status", correlationId);
            long currentTime = System.currentTimeMillis();
            status = WorkflowStatus.builder()
                .correlationId(correlationId)
                .status("PROCESSING")
                .progress(0)
                .message("Workflow is being initialized, please wait...")
                .timestamp(currentTime)
                .lastUpdated(currentTime)
                .build();
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get workflow progress (alias for track endpoint, returns JSON)
     * 
     * @param correlationId The workflow correlation ID
     * @return Current workflow progress
     */
    @GetMapping("/progress/{correlationId}")
    @Operation(
        summary = "Get workflow progress",
        description = "Get current progress of a workflow. Returns the latest status as JSON. " +
                     "Returns a PROCESSING status if the workflow hasn't been tracked yet."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Progress retrieved")
    })
    public ResponseEntity<WorkflowStatus> getProgress(
            @Parameter(description = "Unique correlation ID for the workflow")
            @PathVariable String correlationId) {
        
        log.debug("Getting progress for workflow: {}", correlationId);
        
        WorkflowStatus status = fulfilmentService.getWorkflowStatus(correlationId);
        if (status == null) {
            // Workflow not tracked yet - return default PROCESSING status
            log.debug("Workflow not yet tracked: {} - returning default PROCESSING status", correlationId);
            long currentTime = System.currentTimeMillis();
            status = WorkflowStatus.builder()
                .correlationId(correlationId)
                .status("PROCESSING")
                .progress(0)
                .message("Workflow is being initialized, please wait...")
                .timestamp(currentTime)
                .lastUpdated(currentTime)
                .build();
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get final workflow result
     * 
     * @param correlationId The workflow correlation ID
     * @return Final workflow result if completed
     */
    @GetMapping("/result/{correlationId}")
    @Operation(
        summary = "Get workflow result",
        description = "Get the final result of a completed workflow including credential URLs."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Result retrieved"),
        @ApiResponse(responseCode = "404", description = "Result not available yet or workflow not found")
    })
    public ResponseEntity<WorkflowStatus> getResult(
            @Parameter(description = "Unique correlation ID for the workflow")
            @PathVariable String correlationId) {
        
        log.debug("Getting result for workflow: {}", correlationId);
        
        WorkflowStatus status = fulfilmentService.getWorkflowStatus(correlationId);
        if (status == null) {
            log.warn("Workflow not found: {}", correlationId);
            return ResponseEntity.notFound().build();
        }
        
        // Only return if completed or failed
        if (!"COMPLETED".equals(status.getStatus()) && !"FAILED".equals(status.getStatus())) {
            log.debug("Workflow still in progress: {}", correlationId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get all workflow results
     * 
     * @return Map of all tracked workflows
     */
    @GetMapping("/results")
    @Operation(
        summary = "Get all workflow results",
        description = "Get all tracked workflow results (for debugging/monitoring)"
    )
    public ResponseEntity<Map<String, WorkflowStatus>> getAllResults() {
        log.debug("Getting all workflow results");
        Map<String, WorkflowStatus> results = fulfilmentService.getAllWorkflowStatuses();
        return ResponseEntity.ok(results);
    }
    
    /**
     * Initialize a workflow (for synchronous initialization)
     * This endpoint allows immediate workflow initialization before Kafka events are consumed
     * 
     * @param correlationId The workflow correlation ID
     * @return Success response
     */
    @PostMapping("/initialize/{correlationId}")
    @Operation(
        summary = "Initialize workflow",
        description = "Initialize a workflow immediately to ensure it's tracked before Kafka events are consumed. " +
                     "This is useful for verification workflows that need immediate tracking."
    )
    public ResponseEntity<Map<String, String>> initializeWorkflow(
            @Parameter(description = "Unique correlation ID for the workflow", required = true)
            @PathVariable String correlationId) {
        
        log.info("Initializing workflow synchronously for correlationId: {}", correlationId);
        
        fulfilmentService.initializeWorkflow(
            correlationId, 
            "INITIATED", 
            0, 
            "Verification workflow initiated, processing..."
        );
        
        return ResponseEntity.ok(Map.of(
            "correlationId", correlationId,
            "status", "INITIALIZED",
            "message", "Workflow initialized successfully"
        ));
    }
    
    /**
     * Get system health and statistics
     * 
     * @return System health information
     */
    @GetMapping("/health")
    @Operation(
        summary = "Get fulfilment service health",
        description = "Retrieve health information and statistics about the fulfilment service " +
                     "including active connections and tracked workflows."
    )
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "activeConnections", fulfilmentService.getActiveConnectionsCount(),
            "trackedWorkflows", fulfilmentService.getTrackedWorkflowsCount(),
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(health);
    }
}
