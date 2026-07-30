package pt.ulusofona.digital.wallet.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Base class for all domain entities providing common audit fields.
 * This ensures consistency across all domain objects and provides built-in
 * audit trail capabilities for tracking entity lifecycle.
 */
@Data
@EqualsAndHashCode
@ToString
@Schema(description = "Base entity structure with audit fields")
public abstract class BaseEntity {
    
    /**
     * Unique identifier for this entity.
     * Should be set by the persistence layer.
     */
    @Schema(
        description = "Unique entity identifier",
        example = "12345"
    )
    private String id;
    
    /**
     * Timestamp when the entity was first created.
     * Set automatically by the persistence layer.
     */
    @Schema(
        description = "Entity creation timestamp",
        example = "2025-01-01T12:00:00"
    )
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the entity was last updated.
     * Updated automatically by the persistence layer on modifications.
     */
    @Schema(
        description = "Entity last update timestamp",
        example = "2025-01-01T12:05:00"
    )
    private LocalDateTime updatedAt;
    
    /**
     * User identifier who created this entity.
     * Used for audit trail and access control.
     */
    @Schema(
        description = "User who created the entity",
        example = "user123"
    )
    private String createdBy;
    
    /**
     * User identifier who last updated this entity.
     * Used for audit trail and access control.
     */
    @Schema(
        description = "User who last updated the entity",
        example = "user456"
    )
    private String updatedBy;
    
    /**
     * Version number for optimistic locking.
     * Incremented on each update to prevent concurrent modification conflicts.
     */
    @Schema(
        description = "Entity version for optimistic locking",
        example = "1"
    )
    private Long version;
    
    /**
     * Soft delete flag indicating if the entity is marked as deleted.
     * Allows for data recovery and audit trail maintenance.
     */
    @Schema(
        description = "Soft delete flag",
        example = "false"
    )
    private Boolean deleted = false;
    
    /**
     * Default constructor that initializes audit fields.
     */
    public BaseEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 0L;
        this.deleted = false;
    }
    
    /**
     * Constructor with explicit creation user.
     * 
     * @param createdBy The user creating this entity
     */
    public BaseEntity(String createdBy) {
        this();
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }
    
    /**
     * Updates the audit fields when the entity is modified.
     * Should be called by the persistence layer before saving.
     * 
     * @param updatedBy The user making the update
     */
    public void markAsUpdated(String updatedBy) {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.version++;
    }
    
    /**
     * Marks the entity as soft deleted.
     * 
     * @param deletedBy The user performing the deletion
     */
    public void markAsDeleted(String deletedBy) {
        this.deleted = true;
        markAsUpdated(deletedBy);
    }
    
    /**
     * Restores a soft-deleted entity.
     * 
     * @param restoredBy The user performing the restoration
     */
    public void markAsRestored(String restoredBy) {
        this.deleted = false;
        markAsUpdated(restoredBy);
    }
    
    /**
     * Indicates whether the entity is active (not soft deleted).
     * 
     * @return true if the entity is not deleted
     */
    public boolean isActive() {
        return !deleted;
    }
}

