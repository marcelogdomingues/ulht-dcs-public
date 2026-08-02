package com.example.dcs.sis.clients.hystrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.example.dcs.sis.clients.SisClient;
import com.example.dcs.sis.domain.student.*;

/**
 * Circuit-breaker fallback for {@link SisClient}.
 *
 * <p>Excluded under the {@code demo} profile so that the only {@code SisClient}
 * bean present in demo mode is the {@code @Primary} in-memory mock provided by
 * {@code DemoConfiguration}; this avoids a {@code NoUniqueBeanDefinitionException}
 * from having multiple candidate beans.</p>
 */
@Component
@Profile("!demo")
public class SisFallback implements SisClient {

    private static final Logger logger = LoggerFactory.getLogger(SisFallback.class);

    @Override
    public ResponseEntity<StudentEnrolments> getSIGESEnrolments(Object requestBody) {
        logger.warn("Circuit breaker fallback triggered for getSIGESEnrolments");
        StudentEnrolments fallbackResponse = new StudentEnrolments(0, null, "CIRCUIT_BREAKER_OPEN");
        return ResponseEntity.ok(fallbackResponse);
    }

    @Override
    public ResponseEntity<Object> getUserScheduleSemester(Object requestBody) {
        logger.warn("Circuit breaker fallback triggered for getUserScheduleSemester");
        return ResponseEntity.ok(null); // Schedule doesn't have errorCode field
    }

    @Override
    public ResponseEntity<StudentGrades> getSIGESGrades(Object requestBody) {
        logger.warn("Circuit breaker fallback triggered for getSIGESGrades");
        StudentGrades fallbackResponse = new StudentGrades(null, "CIRCUIT_BREAKER_OPEN");
        return ResponseEntity.ok(fallbackResponse);
    }

    @Override
    public ResponseEntity<Object> login(Object requestBody) {
        logger.warn("Circuit breaker fallback triggered for login");
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<Object> registration(Object requestBody) {
        logger.warn("Circuit breaker fallback triggered for registration");
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<StudentEvals> getSIGESStudentEvals(Object requestBody) {
        logger.warn("Circuit breaker fallback triggered for getSIGESStudentEvals");
        StudentEvals fallbackResponse = new StudentEvals(null, "CIRCUIT_BREAKER_OPEN");
        return ResponseEntity.ok(fallbackResponse);
    }

    @Override
    public ResponseEntity<StudentCourseCredits> getSIGESStudentCourseCredits(Object requestBody) {
        logger.warn("Circuit breaker fallback triggered for getSIGESStudentCourseCredits");
        StudentCourseCredits fallbackResponse = new StudentCourseCredits(0, 0, 0.0, 0, "CIRCUIT_BREAKER_OPEN");
        return ResponseEntity.ok(fallbackResponse);
    }
}