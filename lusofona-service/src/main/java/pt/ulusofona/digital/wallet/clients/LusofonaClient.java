package pt.ulusofona.digital.wallet.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.ResponseEntity;
import pt.ulusofona.digital.wallet.clients.hystrix.LusofonaFallback;
import pt.ulusofona.digital.wallet.config.ClientConfiguration;
import pt.ulusofona.digital.wallet.domain.student.*;


// This Feign client integrates with a university Student Information System (SIS).
// The real base URL and credentials are provided privately via configuration
// (see application.yml / environment); the values in this public repository are
// placeholders.
@FeignClient(name = "lusofonaClient",
        url = "${spring.cloud.openfeign.client.config.lusofonaClient.url}",
        configuration = ClientConfiguration.class,
        fallback = LusofonaFallback.class,
        primary = false)
public interface LusofonaClient {

    @RequestMapping(method = RequestMethod.POST, value = "/GetSIGESEnrolments", consumes = "application/json")
    ResponseEntity<StudentEnrolments> getSIGESEnrolments(@RequestBody Object requestBody);

    @RequestMapping(method = RequestMethod.POST, value = "/GetUserScheduleSemester")
    ResponseEntity<Object> getUserScheduleSemester(@RequestBody Object requestBody);

    @RequestMapping(method = RequestMethod.POST, value = "/GetSIGESGrades")
    ResponseEntity<StudentGrades> getSIGESGrades(@RequestBody Object requestBody);

    @RequestMapping(method = RequestMethod.POST, value = "/Login")
    ResponseEntity<Object> login(@RequestBody Object requestBody);

    @RequestMapping(method = RequestMethod.POST, value = "/Registration")
    ResponseEntity<Object> registration(@RequestBody Object requestBody);

    @RequestMapping(method = RequestMethod.POST, value = "/GetSIGESStudentEvals")
    ResponseEntity<StudentEvals> getSIGESStudentEvals(@RequestBody Object requestBody);

    @RequestMapping(method = RequestMethod.POST, value = "/GetSIGESStudentCourseCredits")
    ResponseEntity<StudentCourseCredits> getSIGESStudentCourseCredits(@RequestBody Object requestBody);

}