package pt.ulusofona.ulht.credential.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity backing the Kafka idempotency guard.
 *
 * <p>One row is written the first time a listener processes an event. The primary
 * key ({@code event_key}) is derived from the record coordinates (topic, partition,
 * offset), so a redelivered record collides on insert and is recognised as a
 * duplicate. See {@link pt.ulusofona.ulht.credential.service.ProcessedEventService}.</p>
 */
@Entity
@Table(name = "processed_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_key", nullable = false, length = 512)
    private String eventKey;

    @Column(name = "consumer")
    private String consumer;

    @Column(name = "processed_at")
    private Instant processedAt;
}
