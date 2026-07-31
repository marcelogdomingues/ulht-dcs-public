package pt.ulusofona.digital.wallet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import pt.ulusofona.digital.wallet.clients.LusofonaClient;
import pt.ulusofona.digital.wallet.domain.student.StudentCourseCredits;
import pt.ulusofona.digital.wallet.domain.student.StudentEnrolments;
import pt.ulusofona.digital.wallet.domain.student.StudentEvals;
import pt.ulusofona.digital.wallet.domain.student.StudentGrades;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DEMO profile configuration for the lusofona-service.
 *
 * <p>When the {@code demo} Spring profile is active, this configuration provides a
 * {@link Primary @Primary} in-memory implementation of {@link LusofonaClient} that returns
 * canned, obviously-fake student data instead of calling the real university Student
 * Information System (SIS). This lets the full issue -> verify pipeline run end-to-end with
 * no external dependencies.</p>
 *
 * <p>The real Feign-generated {@code LusofonaClient} bean is left untouched; because this bean
 * is {@code @Primary} it wins for injection whenever the {@code demo} profile is active. The
 * default and {@code docker} profiles are unaffected.</p>
 *
 * <p>All values here are illustrative placeholders (demo-student, example.edu, ...). They do
 * not represent any real person or institution.</p>
 */
@Configuration
@Profile("demo")
public class DemoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DemoConfiguration.class);

    /**
     * A {@link Primary @Primary} mock of {@link LusofonaClient} that returns canned SIS data.
     *
     * <p>The Kafka credential-issuance flow only calls {@link LusofonaClient#login(Object)} and
     * uses whatever it returns verbatim as the {@code studentData} payload. The downstream
     * credential-service requires at least a {@code studentId} (plus {@code email} and
     * {@code fullName} for wallet creation), so the demo login response includes those. The
     * remaining {@code getSIGES*} methods return canned data so the synchronous REST endpoints
     * also work in demo mode.</p>
     */
    @Bean
    @Primary
    public LusofonaClient demoLusofonaClient() {
        log.warn("⚠️  DEMO profile active - using in-memory mock LusofonaClient (no real SIS calls). "
                + "Student data is illustrative and obviously fake.");
        return new DemoLusofonaClient();
    }

    /**
     * In-memory implementation of {@link LusofonaClient} returning canned demo data.
     */
    static final class DemoLusofonaClient implements LusofonaClient {

        @Override
        public ResponseEntity<StudentEnrolments> getSIGESEnrolments(Object requestBody) {
            StudentEnrolments.Enrolment enrolment = new StudentEnrolments.Enrolment(
                    "2025/2026",
                    "BSc in Computer Science (Demo)",
                    "Distributed Systems",
                    12345,
                    "CS-DS-01",
                    3,
                    6,
                    "Computer Science");
            return ResponseEntity.ok(new StudentEnrolments(1, List.of(enrolment), null));
        }

        @Override
        public ResponseEntity<Object> getUserScheduleSemester(Object requestBody) {
            // Schedule is not needed for the demo credential pipeline; return an empty payload.
            return ResponseEntity.ok(Map.of("count", 0, "schedule", List.of()));
        }

        @Override
        public ResponseEntity<StudentGrades> getSIGESGrades(Object requestBody) {
            StudentGrades.GradeList.Grade grade = new StudentGrades.GradeList.Grade(
                    "2025/2026",
                    3,
                    "Distributed Systems",
                    "Final Exam",
                    "APPROVED",
                    "17",
                    6);
            StudentGrades.GradeList gradeList = new StudentGrades.GradeList("2025/2026", List.of(grade));
            return ResponseEntity.ok(new StudentGrades(List.of(gradeList), null));
        }

        @Override
        public ResponseEntity<Object> login(Object requestBody) {
            // The credential-service reads studentId/email/fullName from this payload.
            // Values are deliberately fake demo placeholders.
            Map<String, Object> studentData = new LinkedHashMap<>();
            studentData.put("studentId", "a12345");
            studentData.put("studentCode", "a12345");
            studentData.put("email", "a12345@students.example.edu");
            studentData.put("name", "Demo Student");
            studentData.put("fullName", "Demo Student");
            studentData.put("courseName", "BSc in Computer Science (Demo)");
            studentData.put("courseCode", 999);
            studentData.put("institutionName", "Example University (Demo)");
            studentData.put("institutionCode", 1);
            studentData.put("degreeName", "Bachelor");
            studentData.put("errorCode", null);
            return ResponseEntity.ok(studentData);
        }

        @Override
        public ResponseEntity<Object> registration(Object requestBody) {
            Map<String, Object> registration = new LinkedHashMap<>();
            registration.put("studentId", "a12345");
            registration.put("email", "a12345@students.example.edu");
            registration.put("fullName", "Demo Student");
            registration.put("errorCode", null);
            return ResponseEntity.ok(registration);
        }

        @Override
        public ResponseEntity<StudentEvals> getSIGESStudentEvals(Object requestBody) {
            StudentEvals.Evaluation.Class.EvaluationDetail detail =
                    new StudentEvals.Evaluation.Class.EvaluationDetail(
                            "Final Exam",
                            1,
                            0L,
                            "Room A1 (Demo Campus)",
                            "120min");
            StudentEvals.Evaluation.Class clazz = new StudentEvals.Evaluation.Class(
                    "CS-DS-01",
                    List.of(detail),
                    "Distributed Systems");
            StudentEvals.Evaluation evaluation = new StudentEvals.Evaluation(0L, 3, List.of(clazz));
            return ResponseEntity.ok(new StudentEvals(List.of(evaluation), null));
        }

        @Override
        public ResponseEntity<StudentCourseCredits> getSIGESStudentCourseCredits(Object requestBody) {
            return ResponseEntity.ok(new StudentCourseCredits(180, 108, 15.5, 16, null));
        }
    }
}
