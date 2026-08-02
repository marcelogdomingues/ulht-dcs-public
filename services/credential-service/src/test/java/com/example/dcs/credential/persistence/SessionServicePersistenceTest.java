package com.example.dcs.credential.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.example.dcs.credential.domain.issuer.RegisteredStudent;
import com.example.dcs.credential.domain.issuer.Session;
import com.example.dcs.credential.service.issuer.SessionService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the JPA-backed {@link SessionService} against an in-memory H2 database
 * (PostgreSQL compatibility mode). Confirms the entity/table mapping and that
 * session/registration behaviour is preserved after replacing the in-memory maps.
 * Runs without Docker via the {@code test} profile.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(SessionService.class)
class SessionServicePersistenceTest {

    @Autowired
    private SessionService sessionService;

    @Test
    void createAndGetSession_persistsAndReturnsSession() {
        Session created = sessionService.createSession(Session.builder()
                .title("Keynote")
                .conferenceName("DevCon")
                .description("Opening keynote")
                .location("Hall A")
                .startTime(OffsetDateTime.parse("2026-08-01T10:00:00+01:00"))
                .endTime(OffsetDateTime.parse("2026-08-01T11:00:00+01:00"))
                .isActive(true)
                .build());

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getRegisteredCount()).isZero();

        Optional<Session> fetched = sessionService.getSession(created.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getTitle()).isEqualTo("Keynote");
        assertThat(fetched.get().getConferenceName()).isEqualTo("DevCon");
        assertThat(fetched.get().getStartTime())
                .isEqualTo(OffsetDateTime.parse("2026-08-01T10:00:00+01:00"));
        assertThat(fetched.get().isActive()).isTrue();
        assertThat(fetched.get().getRegisteredCount()).isZero();
    }

    @Test
    void listSessions_returnsAllPersisted() {
        sessionService.createSession(Session.builder().title("A").conferenceName("C1").build());
        sessionService.createSession(Session.builder().title("B").conferenceName("C2").build());

        List<Session> all = sessionService.getAllSessions();
        assertThat(all).hasSize(2);
    }

    @Test
    void updateSession_preservesIdCreatedAtAndRegisteredCount() {
        Session created = sessionService.createSession(Session.builder()
                .title("Old").conferenceName("Conf").build());
        sessionService.registerStudent(created.getId(), "student-1", "cred-1");

        Session update = Session.builder()
                .title("New")
                .conferenceName("Conf")
                .location("Room 2")
                .isActive(false)
                .build();

        Session updated = sessionService.updateSession(created.getId(), update);
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getRegisteredCount()).isEqualTo(1);

        Session refetched = sessionService.getSession(created.getId()).orElseThrow();
        assertThat(refetched.getTitle()).isEqualTo("New");
        assertThat(refetched.isActive()).isFalse();
        assertThat(refetched.getRegisteredCount()).isEqualTo(1);
    }

    @Test
    void updateSession_missing_throws() {
        assertThrows(java.util.NoSuchElementException.class,
                () -> sessionService.updateSession("missing", Session.builder().build()));
    }

    @Test
    void registerStudent_isIdempotentPerStudentAndUpdatesCount() {
        Session created = sessionService.createSession(Session.builder()
                .title("Workshop").conferenceName("Conf").build());

        sessionService.registerStudent(created.getId(), "student-1", "cred-1");
        sessionService.registerStudent(created.getId(), "student-1", "cred-1"); // duplicate ignored
        sessionService.registerStudent(created.getId(), "student-2", "cred-2");

        assertThat(sessionService.getSession(created.getId()).orElseThrow().getRegisteredCount())
                .isEqualTo(2);

        List<RegisteredStudent> students = sessionService.getRegisteredStudents(created.getId());
        assertThat(students).hasSize(2);
        assertThat(students).allSatisfy(s -> {
            assertThat(s.getSessionId()).isEqualTo(created.getId());
            assertThat(s.getEmail()).endsWith("@students.example.edu");
            assertThat(s.getRegisteredAt()).isNotNull();
        });
    }

    @Test
    void registerStudent_missingSession_throws() {
        assertThrows(java.util.NoSuchElementException.class,
                () -> sessionService.registerStudent("missing", "student-1", "cred-1"));
    }

    @Test
    void deleteSession_removesSessionAndRegistrations() {
        Session created = sessionService.createSession(Session.builder()
                .title("Temp").conferenceName("Conf").build());
        sessionService.registerStudent(created.getId(), "student-1", "cred-1");

        sessionService.deleteSession(created.getId());

        assertThat(sessionService.getSession(created.getId())).isEmpty();
        assertThat(sessionService.getRegisteredStudents(created.getId())).isEmpty();
    }
}
