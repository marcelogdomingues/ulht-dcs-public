package com.example.dcs.credential.domain.issuer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a conference session that students can register for.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private String id;
    private String title;
    private String description;
    private String conferenceName;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String location;
    private String qrCodeUrl;
    private int registeredCount;
    private Instant createdAt;
    private boolean isActive;

    public static Session create(String title, String conferenceName) {
        return Session.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .conferenceName(conferenceName)
                .registeredCount(0)
                .createdAt(Instant.now())
                .isActive(true)
                .build();
    }
}

