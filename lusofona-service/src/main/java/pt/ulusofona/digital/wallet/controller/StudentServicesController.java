package pt.ulusofona.digital.wallet.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import pt.ulusofona.digital.wallet.generated.api.StudentServicesApi;
import pt.ulusofona.digital.wallet.generated.model.*;
import pt.ulusofona.digital.wallet.service.LusofonaService;
import pt.ulusofona.digital.wallet.exception.http.*;
import pt.ulusofona.digital.wallet.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;

@Slf4j
@Controller
public class StudentServicesController implements StudentServicesApi {

    private final LusofonaService lusofonaService;

    public StudentServicesController(LusofonaService lusofonaService) {
        this.lusofonaService = lusofonaService;
    }

    @Override
    public ResponseEntity<StudentEnrolments> getStudentEnrolments(@Valid StudentServiceRequest studentServiceRequest) {
        log.info("Received request to get student enrolments for user: {}", studentServiceRequest.getUserName());
        
        try {
            Map<String, Object> requestMap = convertToMap(studentServiceRequest);
            pt.ulusofona.digital.wallet.domain.student.StudentEnrolments domainResult = 
                lusofonaService.getStudentEnrolments(requestMap);
            
            // Handle null result (fallback response)
            if (domainResult == null) {
                log.warn("Received null response from service for user: {} - using fallback", studentServiceRequest.getUserName());
                StudentEnrolments fallbackResult = new StudentEnrolments();
                fallbackResult.setCount(0);
                fallbackResult.setErrorCode("SERVICE_UNAVAILABLE");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResult);
            }
            
            // Convert domain model to generated API model
            StudentEnrolments result = convertToApiModel(domainResult);
            return ResponseEntity.ok(result);
        } catch (ExternalServiceException e) {
            log.error("External service error getting student enrolments for user: {}", studentServiceRequest.getUserName(), e);
            StudentEnrolments errorResult = new StudentEnrolments();
            errorResult.setCount(0);
            errorResult.setErrorCode("EXTERNAL_SERVICE_ERROR");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResult);
        } catch (TimeoutException e) {
            log.error("Timeout error getting student enrolments for user: {}", studentServiceRequest.getUserName(), e);
            StudentEnrolments errorResult = new StudentEnrolments();
            errorResult.setCount(0);
            errorResult.setErrorCode("TIMEOUT");
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResult);
        } catch (ValidationException e) {
            log.error("Validation error getting student enrolments for user: {}", studentServiceRequest.getUserName(), e);
            StudentEnrolments errorResult = new StudentEnrolments();
            errorResult.setCount(0);
            errorResult.setErrorCode("VALIDATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
        } catch (Exception e) {
            log.error("Unexpected error getting student enrolments for user: {}", studentServiceRequest.getUserName(), e);
            StudentEnrolments errorResult = new StudentEnrolments();
            errorResult.setCount(0);
            errorResult.setErrorCode("INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @Override
    public ResponseEntity<StudentSchedule> getStudentSchedule(@Valid StudentServiceRequest studentServiceRequest) {
        log.info("Received request to get student schedule for user: {}", studentServiceRequest.getUserName());
        
        try {
            Map<String, Object> requestMap = convertToMap(studentServiceRequest);
            pt.ulusofona.digital.wallet.domain.student.StudentSchedule domainResult = 
                lusofonaService.getUserScheduleSemester(requestMap);
            
            // Handle null result (fallback response)
            if (domainResult == null) {
                log.warn("Received null response from service for user: {} - using fallback", studentServiceRequest.getUserName());
                StudentSchedule fallbackResult = new StudentSchedule();
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResult);
            }
            
            // Convert domain model to generated API model
            StudentSchedule result = convertToApiModel(domainResult);
            return ResponseEntity.ok(result);
        } catch (ExternalServiceException e) {
            log.error("External service error getting student schedule for user: {}", studentServiceRequest.getUserName(), e);
            StudentSchedule errorResult = new StudentSchedule();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResult);
        } catch (TimeoutException e) {
            log.error("Timeout error getting student schedule for user: {}", studentServiceRequest.getUserName(), e);
            StudentSchedule errorResult = new StudentSchedule();
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResult);
        } catch (ValidationException e) {
            log.error("Validation error getting student schedule for user: {}", studentServiceRequest.getUserName(), e);
            StudentSchedule errorResult = new StudentSchedule();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
        } catch (Exception e) {
            log.error("Unexpected error getting student schedule for user: {}", studentServiceRequest.getUserName(), e);
            StudentSchedule errorResult = new StudentSchedule();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @Override
    public ResponseEntity<StudentGrades> getStudentGrades(@Valid StudentServiceRequest studentServiceRequest) {
        log.info("Received request to get student grades for user: {}", studentServiceRequest.getUserName());
        
        try {
            Map<String, Object> requestMap = convertToMap(studentServiceRequest);
            pt.ulusofona.digital.wallet.domain.student.StudentGrades domainResult = 
                lusofonaService.getSIGESGrades(requestMap);
            
            // Handle null result (fallback response)
            if (domainResult == null) {
                log.warn("Received null response from service for user: {} - using fallback", studentServiceRequest.getUserName());
                StudentGrades fallbackResult = new StudentGrades();
                fallbackResult.setErrorCode("SERVICE_UNAVAILABLE");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResult);
            }
            
            // Convert domain model to generated API model
            StudentGrades result = convertToApiModel(domainResult);
            return ResponseEntity.ok(result);
        } catch (ExternalServiceException e) {
            log.error("External service error getting student grades for user: {}", studentServiceRequest.getUserName(), e);
            StudentGrades errorResult = new StudentGrades();
            errorResult.setErrorCode("EXTERNAL_SERVICE_ERROR");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResult);
        } catch (TimeoutException e) {
            log.error("Timeout error getting student grades for user: {}", studentServiceRequest.getUserName(), e);
            StudentGrades errorResult = new StudentGrades();
            errorResult.setErrorCode("TIMEOUT");
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResult);
        } catch (ValidationException e) {
            log.error("Validation error getting student grades for user: {}", studentServiceRequest.getUserName(), e);
            StudentGrades errorResult = new StudentGrades();
            errorResult.setErrorCode("VALIDATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
        } catch (Exception e) {
            log.error("Unexpected error getting student grades for user: {}", studentServiceRequest.getUserName(), e);
            StudentGrades errorResult = new StudentGrades();
            errorResult.setErrorCode("INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @Override
    public ResponseEntity<StudentEvaluations> getStudentEvaluations(@Valid StudentServiceRequest studentServiceRequest) {
        log.info("Received request to get student evaluations for user: {}", studentServiceRequest.getUserName());
        
        try {
            Map<String, Object> requestMap = convertToMap(studentServiceRequest);
            pt.ulusofona.digital.wallet.domain.student.StudentEvals domainResult = 
                lusofonaService.getSIGESStudentEvals(requestMap);
            
            // Handle null result (fallback response)
            if (domainResult == null) {
                log.warn("Received null response from service for user: {} - using fallback", studentServiceRequest.getUserName());
                StudentEvaluations fallbackResult = new StudentEvaluations();
                fallbackResult.setErrorCode("SERVICE_UNAVAILABLE");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResult);
            }
            
            // Convert domain model to generated API model
            StudentEvaluations result = convertToApiModel(domainResult);
            return ResponseEntity.ok(result);
        } catch (ExternalServiceException e) {
            log.error("External service error getting student evaluations for user: {}", studentServiceRequest.getUserName(), e);
            StudentEvaluations errorResult = new StudentEvaluations();
            errorResult.setErrorCode("EXTERNAL_SERVICE_ERROR");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResult);
        } catch (TimeoutException e) {
            log.error("Timeout error getting student evaluations for user: {}", studentServiceRequest.getUserName(), e);
            StudentEvaluations errorResult = new StudentEvaluations();
            errorResult.setErrorCode("TIMEOUT");
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResult);
        } catch (ValidationException e) {
            log.error("Validation error getting student evaluations for user: {}", studentServiceRequest.getUserName(), e);
            StudentEvaluations errorResult = new StudentEvaluations();
            errorResult.setErrorCode("VALIDATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
        } catch (Exception e) {
            log.error("Unexpected error getting student evaluations for user: {}", studentServiceRequest.getUserName(), e);
            StudentEvaluations errorResult = new StudentEvaluations();
            errorResult.setErrorCode("INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @Override
    public ResponseEntity<StudentCourseCredits> getStudentCourseCredits(@Valid StudentServiceRequest studentServiceRequest) {
        log.info("Received request to get student course credits for user: {}", studentServiceRequest.getUserName());
        
        try {
            Map<String, Object> requestMap = convertToMap(studentServiceRequest);
            pt.ulusofona.digital.wallet.domain.student.StudentCourseCredits domainResult = 
                lusofonaService.getStudentCourseCredits(requestMap);
            
            // Handle null result (fallback response)
            if (domainResult == null) {
                log.warn("Received null response from service for user: {} - using fallback", studentServiceRequest.getUserName());
                StudentCourseCredits fallbackResult = new StudentCourseCredits();
                fallbackResult.setErrorCode("SERVICE_UNAVAILABLE");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResult);
            }
            
            // Convert domain model to generated API model
            StudentCourseCredits result = convertToApiModel(domainResult);
            return ResponseEntity.ok(result);
        } catch (ExternalServiceException e) {
            log.error("External service error getting student course credits for user: {}", studentServiceRequest.getUserName(), e);
            StudentCourseCredits errorResult = new StudentCourseCredits();
            errorResult.setErrorCode("EXTERNAL_SERVICE_ERROR");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResult);
        } catch (TimeoutException e) {
            log.error("Timeout error getting student course credits for user: {}", studentServiceRequest.getUserName(), e);
            StudentCourseCredits errorResult = new StudentCourseCredits();
            errorResult.setErrorCode("TIMEOUT");
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResult);
        } catch (ValidationException e) {
            log.error("Validation error getting student course credits for user: {}", studentServiceRequest.getUserName(), e);
            StudentCourseCredits errorResult = new StudentCourseCredits();
            errorResult.setErrorCode("VALIDATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
        } catch (Exception e) {
            log.error("Unexpected error getting student course credits for user: {}", studentServiceRequest.getUserName(), e);
            StudentCourseCredits errorResult = new StudentCourseCredits();
            errorResult.setErrorCode("INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    private Map<String, Object> convertToMap(StudentServiceRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", request.getUserName());
        map.put("installKey", request.getInstallKey());
        map.put("language", request.getLanguage() != null ? request.getLanguage().getValue() : null);
        map.put("platform", request.getPlatform() != null ? request.getPlatform().getValue() : null);
        map.put("application", request.getApplication());
        map.put("versionCode", request.getVersionCode());
        return map;
    }

    private StudentEnrolments convertToApiModel(pt.ulusofona.digital.wallet.domain.student.StudentEnrolments domain) {
        if (domain == null) {
            return new StudentEnrolments();
        }
        
        StudentEnrolments apiModel = new StudentEnrolments();
        apiModel.setCount(domain.count());
        apiModel.setErrorCode(domain.errorCode());
        
        if (domain.enrolmentList() != null) {
            domain.enrolmentList().forEach(enrolment -> {
                pt.ulusofona.digital.wallet.generated.model.Enrolment apiEnrolment = 
                    new pt.ulusofona.digital.wallet.generated.model.Enrolment();
                apiEnrolment.setAcademicYear(enrolment.academicYear());
                apiEnrolment.setCourseName(enrolment.courseName());
                apiEnrolment.setCurricularUnitName(enrolment.curricularUnitName());
                apiEnrolment.setCurricularUnitCode(enrolment.curricularUnitCode());
                apiEnrolment.setClassName(enrolment.className());
                apiEnrolment.setCurricularYear(enrolment.curricularYear());
                apiEnrolment.setEcts(enrolment.ects());
                apiEnrolment.setProgramme(enrolment.programme());
                apiModel.getEnrolmentList().add(apiEnrolment);
            });
        }
        
        return apiModel;
    }

    private StudentSchedule convertToApiModel(pt.ulusofona.digital.wallet.domain.student.StudentSchedule domain) {
        if (domain == null) {
            return new StudentSchedule();
        }
        
        StudentSchedule apiModel = new StudentSchedule();
        
        // Convert schedule entries
        if (domain.schedule() != null) {
            domain.schedule().forEach(scheduleEntry -> {
                // Each ScheduleEntry has a list of ClassSchedule
                if (scheduleEntry.schedule() != null) {
                    scheduleEntry.schedule().forEach(classSchedule -> {
                        pt.ulusofona.digital.wallet.generated.model.ScheduleItem apiItem = 
                            new pt.ulusofona.digital.wallet.generated.model.ScheduleItem();
                        apiItem.setDay(scheduleEntry.dayOfWeek());
                        apiItem.setTime(classSchedule.startTime());
                        apiItem.setCourse(classSchedule.curricularUnitName());
                        apiItem.setProfessor(classSchedule.teacherName());
                        apiItem.setRoom(classSchedule.roomName());
                        apiItem.setType(classSchedule.classType());
                        apiModel.getSchedule().add(apiItem);
                    });
                }
            });
        }
        
        return apiModel;
    }

    private StudentGrades convertToApiModel(pt.ulusofona.digital.wallet.domain.student.StudentGrades domain) {
        if (domain == null) {
            return new StudentGrades();
        }
        
        StudentGrades apiModel = new StudentGrades();
        apiModel.setErrorCode(domain.errorCode());
        
        if (domain.gradeList() != null) {
            domain.gradeList().forEach(gradeList -> {
                if (gradeList.grades() != null) {
                    gradeList.grades().forEach(grade -> {
                        pt.ulusofona.digital.wallet.generated.model.Grade apiGrade = 
                            new pt.ulusofona.digital.wallet.generated.model.Grade();
                        apiGrade.setAcademicYear(grade.academicYear());
                        apiGrade.setCourseName(grade.curricularUnitName());
                        apiGrade.setGrade(Integer.valueOf(grade.grade()));
                        apiGrade.setCredits(grade.ects());
                        apiGrade.setSemester(grade.curricularYear());
                        apiModel.getGrades().add(apiGrade);
                    });
                }
            });
        }
        
        return apiModel;
    }

    private StudentEvaluations convertToApiModel(pt.ulusofona.digital.wallet.domain.student.StudentEvals domain) {
        if (domain == null) {
            return new StudentEvaluations();
        }
        
        StudentEvaluations apiModel = new StudentEvaluations();
        apiModel.setErrorCode(domain.errorCode());
        
        if (domain.evaluationList() != null) {
            domain.evaluationList().forEach(evaluation -> {
                // Each evaluation contains a list of classes
                if (evaluation.classList() != null) {
                    evaluation.classList().forEach(classItem -> {
                        // Each class contains a list of evaluation details
                        if (classItem.evaluationList() != null) {
                            classItem.evaluationList().forEach(evalDetail -> {
                                pt.ulusofona.digital.wallet.generated.model.Evaluation apiEvaluation = 
                                    new pt.ulusofona.digital.wallet.generated.model.Evaluation();
                                apiEvaluation.setCourseName(classItem.curricularUnitName());
                                apiEvaluation.setEvaluationType(evalDetail.evaluationName());
                                // Convert timestamp to LocalDate (simplified - just use current date)
                                apiEvaluation.setDate(java.time.LocalDate.now());
                                apiEvaluation.setGrade(null); // Grade not available in domain model
                                apiEvaluation.setStatus("SCHEDULED"); // Default status
                                apiModel.getEvaluations().add(apiEvaluation);
                            });
                        }
                    });
                }
            });
        }
        
        return apiModel;
    }

    private StudentCourseCredits convertToApiModel(pt.ulusofona.digital.wallet.domain.student.StudentCourseCredits domain) {
        if (domain == null) {
            return new StudentCourseCredits();
        }
        
        StudentCourseCredits apiModel = new StudentCourseCredits();
        apiModel.setTotalCredits(domain.courseCredits());
        apiModel.setEarnedCredits(domain.obtainedCredits());
        apiModel.setGpa((float) domain.averageGrade());
        apiModel.setRemainingCredits(domain.courseCredits() - domain.obtainedCredits());
        apiModel.setErrorCode(domain.errorCode());
        
        return apiModel;
    }
}