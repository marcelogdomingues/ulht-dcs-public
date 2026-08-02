package com.example.dcs.credential.persistence;

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
 * JPA entity that durably stores an issuer session, replacing the in-memory
 * {@code sessions} map previously held in {@code SessionService}.
 *
 * <p>Nested/complex temporal fields such as {@code startTime} / {@code endTime}
 * (originally {@link java.time.OffsetDateTime}) are persisted as ISO-8601 text
 * to stay portable across PostgreSQL and the H2 test database and to keep the
 * mapping explicit at the service boundary.</p>
 */
@Entity
@Table(name = "issuer_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuerSessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "conference_name")
    private String conferenceName;

    /** ISO-8601 offset date-time text (e.g. 2026-08-01T10:00:00+01:00). */
    @Column(name = "start_time")
    private String startTime;

    /** ISO-8601 offset date-time text. */
    @Column(name = "end_time")
    private String endTime;

    @Column(name = "location")
    private String location;

    @Column(name = "qr_code_url", columnDefinition = "text")
    private String qrCodeUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "is_active")
    private boolean active;
}
