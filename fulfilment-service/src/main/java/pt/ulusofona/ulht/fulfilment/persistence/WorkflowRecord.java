package pt.ulusofona.ulht.fulfilment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity that durably stores the workflow status previously held in an
 * in-memory map. The {@code result} field is persisted as JSON text and is
 * serialized/deserialized at the service boundary using the Jackson 2
 * ObjectMapper bean provided by AppConfiguration.
 */
@Entity
@Table(name = "workflow_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRecord {

    @Id
    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "status")
    private String status;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "message")
    private String message;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_name")
    private String errorName;

    @Column(name = "error_message")
    private String errorMessage;

    /** Workflow result serialized as JSON text. */
    @Column(name = "result", columnDefinition = "text")
    private String result;

    @Column(name = "timestamp")
    private Long timestamp;

    @Column(name = "last_updated")
    private Long lastUpdated;

    @Version
    @Column(name = "version")
    private Long version;
}
