package pt.ulusofona.ulht.credential.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulusofona.ulht.credential.persistence.ProcessedEventEntity;
import pt.ulusofona.ulht.credential.persistence.ProcessedEventRepository;

import java.time.Instant;

/**
 * Lightweight idempotency guard for Kafka consumers (at-least-once safety).
 *
 * <p>Kafka guarantees at-least-once delivery, so a consumer can legitimately see the
 * same record more than once (e.g. after a rebalance or a redelivery following a
 * transient failure and retry). This service records the coordinates of every event a
 * listener has already handled in the {@code processed_event} table and lets the
 * listener short-circuit duplicates.</p>
 *
 * <p>The event key is {@code topic-partition-offset}, which uniquely identifies a
 * physical record on the broker and is therefore a robust dedupe key regardless of
 * payload contents.</p>
 *
 * <p><b>Fail-open by design:</b> if the datastore is unavailable the guard logs and
 * returns {@code false} (treat as not-yet-processed) so the happy path is never blocked
 * by the idempotency layer - at worst a duplicate is reprocessed, which is the exact
 * behaviour we have today.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository repository;

    /**
     * Atomically records that a record has been processed and reports whether it was
     * <em>already</em> processed before this call.
     *
     * @param topic     Kafka topic the record came from
     * @param partition partition the record came from
     * @param offset    offset of the record
     * @param consumer  logical consumer/listener name (for diagnostics only)
     * @return {@code true} if this event was already processed (caller should skip),
     *         {@code false} if it is new (caller should process it)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean isDuplicate(String topic, int partition, long offset, String consumer) {
        String eventKey = topic + "-" + partition + "-" + offset;
        try {
            if (repository.existsById(eventKey)) {
                log.info("Skipping duplicate event {} (already processed by {})", eventKey, consumer);
                return true;
            }
            repository.save(ProcessedEventEntity.builder()
                    .eventKey(eventKey)
                    .consumer(consumer)
                    .processedAt(Instant.now())
                    .build());
            return false;
        } catch (DataIntegrityViolationException e) {
            // Concurrent processing of the same record inserted the row first - it's a duplicate.
            log.info("Skipping duplicate event {} (concurrent insert detected for {})", eventKey, consumer);
            return true;
        } catch (Exception e) {
            // Fail open: never let the idempotency layer break the happy path.
            log.warn("Idempotency check failed for event {} ({}); processing anyway: {}",
                    eventKey, consumer, e.getMessage());
            return false;
        }
    }
}
