package pt.ulusofona.ulht.fulfilment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pt.ulusofona.ulht.fulfilment.domain.WorkflowStatus;
import pt.ulusofona.ulht.fulfilment.domain.WorkflowProgressEvent;
import pt.ulusofona.ulht.fulfilment.domain.WorkflowCompletionEvent;
import pt.ulusofona.ulht.fulfilment.domain.WorkflowErrorEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fulfilment Service handles the completion of credential workflows and notifies clients.
 * This service manages the "end" of the workflow that was missing from the architecture.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FulfilmentService {
    
    // Store active SSE connections for real-time updates
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    private final Map<String, WorkflowStatus> workflowStatuses = new ConcurrentHashMap<>();
    
    // Executor for cleanup tasks
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    
    @Value("${fulfilment-service.sse.timeout:30000}")
    private long sseTimeout;
    
    @Value("${fulfilment-service.sse.cleanup-delay:300}")
    private int cleanupDelaySeconds;
    
    /**
     * Creates a new SSE connection for workflow tracking
     * 
     * @param correlationId The workflow correlation ID
     * @return SseEmitter for real-time updates
     */
    public SseEmitter createConnection(String correlationId) {
        SseEmitter emitter = new SseEmitter(sseTimeout);
        
        // Handle connection lifecycle
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for correlationId: {}", correlationId);
            activeConnections.remove(correlationId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out for correlationId: {}", correlationId);
            activeConnections.remove(correlationId);
        });
        
        emitter.onError(throwable -> {
            log.error("SSE connection error for correlationId: {}", correlationId, throwable);
            activeConnections.remove(correlationId);
        });
        
        activeConnections.put(correlationId, emitter);
        
        // Initialize workflow status
        long currentTime = System.currentTimeMillis();
        workflowStatuses.put(correlationId, WorkflowStatus.builder()
            .correlationId(correlationId)
            .status("CONNECTED")
            .progress(0)
            .message("Connected to workflow tracking")
            .timestamp(currentTime)
            .lastUpdated(currentTime)
            .build());
        
        // Send initial status
        try {
            emitter.send(WorkflowProgressEvent.builder()
                .correlationId(correlationId)
                .status("CONNECTED")
                .progress(0)
                .message("Connected to workflow tracking")
                .timestamp(System.currentTimeMillis())
                .build());
        } catch (IOException e) {
            log.error("Failed to send initial status for correlationId: {}", correlationId, e);
        }
        
        return emitter;
    }
    
    /**
     * Initializes a workflow if it doesn't exist
     * This ensures the workflow is tracked immediately, even before Kafka events are consumed
     * 
     * @param correlationId The workflow correlation ID
     * @param status Initial status
     * @param progress Initial progress (0-100)
     * @param message Initial message
     */
    public void initializeWorkflow(String correlationId, String status, int progress, String message) {
        if (workflowStatuses.containsKey(correlationId)) {
            log.debug("Workflow already exists for correlationId: {}", correlationId);
            return;
        }
        
        log.info("Initializing workflow for correlationId: {} with status: {}", correlationId, status);
        long currentTime = System.currentTimeMillis();
        workflowStatuses.put(correlationId, WorkflowStatus.builder()
            .correlationId(correlationId)
            .status(status)
            .progress(progress)
            .message(message)
            .timestamp(currentTime)
            .lastUpdated(currentTime)
            .build());
    }
    
    /**
     * Publishes workflow progress updates
     * 
     * @param correlationId The workflow correlation ID
     * @param status Current status
     * @param progress Progress percentage (0-100)
     * @param message Status message
     */
    public void publishProgress(String correlationId, String status, int progress, String message) {
        log.info("Publishing progress for {}: {} ({}%) - {}", correlationId, status, progress, message);
        
        // Get existing status to preserve timestamp if it exists
        WorkflowStatus existingStatus = workflowStatuses.get(correlationId);
        long currentTime = System.currentTimeMillis();
        Long existingTimestamp = (existingStatus != null && existingStatus.getTimestamp() != null) 
            ? existingStatus.getTimestamp() 
            : currentTime;
        
        // Update workflow status
        workflowStatuses.put(correlationId, WorkflowStatus.builder()
            .correlationId(correlationId)
            .status(status)
            .progress(progress)
            .message(message)
            .timestamp(existingTimestamp)
            .lastUpdated(currentTime)
            .build());
        
        // Send to SSE connection if exists
        SseEmitter emitter = activeConnections.get(correlationId);
        if (emitter != null) {
            try {
                WorkflowProgressEvent event = WorkflowProgressEvent.builder()
                    .correlationId(correlationId)
                    .status(status)
                    .progress(progress)
                    .message(message)
                    .timestamp(System.currentTimeMillis())
                    .build();
                
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(event));
                    
                log.debug("Sent progress update to SSE connection for {}", correlationId);
            } catch (IOException e) {
                log.error("Failed to send progress update for correlationId: {}", correlationId, e);
                activeConnections.remove(correlationId);
            }
        }
    }
    
    /**
     * Publishes workflow completion
     * 
     * @param correlationId The workflow correlation ID
     * @param result The completion result
     */
    public void publishCompletion(String correlationId, Object result) {
        log.info("Publishing completion for {}: {}", correlationId, result);
        
        // Get existing status to preserve timestamp if it exists
        WorkflowStatus existingStatus = workflowStatuses.get(correlationId);
        long currentTime = System.currentTimeMillis();
        Long existingTimestamp = (existingStatus != null && existingStatus.getTimestamp() != null) 
            ? existingStatus.getTimestamp() 
            : currentTime;
        
        // Update workflow status (create if doesn't exist)
        workflowStatuses.put(correlationId, WorkflowStatus.builder()
            .correlationId(correlationId)
            .status("COMPLETED")
            .progress(100)
            .message("Workflow completed successfully")
            .result(result)
            .timestamp(existingTimestamp)
            .lastUpdated(currentTime)
            .build());
        
        // Send to SSE connection if exists
        SseEmitter emitter = activeConnections.get(correlationId);
        if (emitter != null) {
            try {
                WorkflowCompletionEvent event = WorkflowCompletionEvent.builder()
                    .correlationId(correlationId)
                    .status("COMPLETED")
                    .progress(100)
                    .message("Workflow completed successfully")
                    .result(result)
                    .timestamp(System.currentTimeMillis())
                    .build();
                
                emitter.send(SseEmitter.event()
                    .name("completed")
                    .data(event));
                    
                emitter.complete();
                log.info("Sent completion event and closed SSE connection for {}", correlationId);
            } catch (IOException e) {
                log.error("Failed to send completion event for correlationId: {}", correlationId, e);
            } finally {
                activeConnections.remove(correlationId);
            }
        }
        
        // Schedule cleanup
        scheduleCleanup(correlationId, cleanupDelaySeconds);
    }
    
    /**
     * Publishes workflow error
     * 
     * @param correlationId The workflow correlation ID
     * @param errorMessage Error message
     * @param errorCode Error code
     */
    public void publishError(String correlationId, String errorMessage, String errorCode) {
        publishError(correlationId, errorMessage, errorCode, null);
    }
    
    /**
     * Publishes workflow error with error name
     * 
     * @param correlationId The workflow correlation ID
     * @param errorMessage Error message
     * @param errorCode Error code
     * @param errorName Optional error name
     */
    public void publishError(String correlationId, String errorMessage, String errorCode, String errorName) {
        log.error("Publishing error for {}: {} ({}) - {}", correlationId, errorCode, errorName, errorMessage);
        
        // Get existing status to preserve timestamp if it exists
        WorkflowStatus existingStatus = workflowStatuses.get(correlationId);
        long currentTime = System.currentTimeMillis();
        Long existingTimestamp = (existingStatus != null && existingStatus.getTimestamp() != null) 
            ? existingStatus.getTimestamp() 
            : currentTime;
        
        // Update workflow status (create if doesn't exist)
        workflowStatuses.put(correlationId, WorkflowStatus.builder()
            .correlationId(correlationId)
            .status("FAILED")
            .progress(-1)
            .message("Workflow failed: " + errorMessage)
            .errorCode(errorCode)
            .errorName(errorName)
            .errorMessage(errorMessage)
            .timestamp(existingTimestamp)
            .lastUpdated(currentTime)
            .build());
        
        // Send to SSE connection if exists
        SseEmitter emitter = activeConnections.get(correlationId);
        if (emitter != null) {
            try {
                WorkflowErrorEvent event = WorkflowErrorEvent.builder()
                    .correlationId(correlationId)
                    .status("FAILED")
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .timestamp(System.currentTimeMillis())
                    .build();
                
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(event));
                    
                emitter.completeWithError(new RuntimeException(errorMessage));
                log.error("Sent error event and closed SSE connection for {}", correlationId);
            } catch (IOException e) {
                log.error("Failed to send error event for correlationId: {}", correlationId, e);
            } finally {
                activeConnections.remove(correlationId);
            }
        }
        
        // Schedule cleanup
        scheduleCleanup(correlationId, cleanupDelaySeconds);
    }
    
    /**
     * Gets the current status of a workflow
     * 
     * @param correlationId The workflow correlation ID
     * @return Current workflow status
     */
    public WorkflowStatus getWorkflowStatus(String correlationId) {
        return workflowStatuses.get(correlationId);
    }
    
    /**
     * Schedules cleanup of workflow data
     * 
     * @param correlationId The workflow correlation ID
     * @param delaySeconds Delay in seconds before cleanup
     */
    private void scheduleCleanup(String correlationId, int delaySeconds) {
        executorService.schedule(() -> {
            log.debug("Cleaning up workflow data for {}", correlationId);
            workflowStatuses.remove(correlationId);
            activeConnections.remove(correlationId);
        }, delaySeconds, TimeUnit.SECONDS);
    }
    
    /**
     * Gets the number of active connections
     * 
     * @return Number of active SSE connections
     */
    public int getActiveConnectionsCount() {
        return activeConnections.size();
    }
    
    /**
     * Gets the number of tracked workflows
     * 
     * @return Number of tracked workflows
     */
    public int getTrackedWorkflowsCount() {
        return workflowStatuses.size();
    }
    
    /**
     * Gets all workflow statuses
     * 
     * @return Map of all workflow statuses
     */
    public Map<String, WorkflowStatus> getAllWorkflowStatuses() {
        return Map.copyOf(workflowStatuses);
    }
}
