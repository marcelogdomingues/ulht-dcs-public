package pt.ulusofona.digital.wallet.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import pt.ulusofona.digital.wallet.clients.LusofonaClient;
import pt.ulusofona.digital.wallet.domain.avro.UniversityRegistrationRequest;
import pt.ulusofona.digital.wallet.domain.avro.UniversityServiceCommonRequest;
import pt.ulusofona.digital.wallet.domain.student.*;
import pt.ulusofona.digital.wallet.exception.ErrorCodes;
import pt.ulusofona.digital.wallet.exception.ErrorUtils;
import pt.ulusofona.digital.wallet.exception.ValidationException;
import pt.ulusofona.digital.wallet.exception.http.*;
import pt.ulusofona.digital.wallet.exception.lusofona.LusofonaApiException;
import pt.ulusofona.digital.wallet.exception.lusofona.LusofonaResponseValidator;

import java.util.Map;
import java.util.function.Supplier;

@Service
public class LusofonaService extends ServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(LusofonaService.class);

    private final LusofonaClient lusofonaClient;
    private final ConversionService conversionService;

    public LusofonaService(LusofonaClient lusofonaClient,
                           @Qualifier("mvcConversionService") ConversionService conversionService) { // Add @Qualifier here
        this.lusofonaClient = lusofonaClient;
        this.conversionService = conversionService;
    }

    private <T> T executeAndSendMessage(final Map<String, Object> requestBody,
                                        final Supplier<T> operation,
                                        final String successMessage,
                                        final String errorMessage) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, LusofonaApiException {
        // Validate the incoming request body.
        validateRequest(requestBody);
        logger.info("Attempting to execute operation: {}", successMessage);

        try {
            Object commonRequest;
            // Convert requestBody into UniversityServiceCommonRequest.
            // NOTE: never log requestBody / commonRequest - they contain credentials
            // (installKey, password, email) and other PII.
            if (requestBody.get("installKey") == null) {
                logger.info("No installKey present, using registration conversion for operation: {}", successMessage);
                commonRequest = conversionService.convert(requestBody, UniversityRegistrationRequest.class);
            } else {
                logger.info("installKey present, using service conversion for operation: {}", successMessage);
                commonRequest = conversionService.convert(requestBody, UniversityServiceCommonRequest.class);
            }

            // Execute the provided asynchronous operation.
            T result = executeAsync(operation, errorMessage);

            logger.info("Operation successful: {}", successMessage);
            return result;
        } catch (Exception e) {
            // Log the error using clean logging without stack trace
            ErrorUtils.logError(ErrorCodes.EXTERNAL_SERVICE_ERROR, "Operation failed: " + errorMessage, e);
            // Re-throw the exception to be handled by upstream layers (e.g., controllers or global exception handlers).
            throw e;
        }
    }

    public StudentCourseCredits getStudentCourseCredits(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, LusofonaApiException {
        StudentCourseCredits response = executeAndSendMessage(
                requestBody,
                () -> lusofonaClient.getSIGESStudentCourseCredits(requestBody).getBody(),
                "Student course credits fetched",
                "Error fetching student course credits"
        );
        LusofonaResponseValidator.validateStudentCourseCredits(response, "Get Student Course Credits");
        return response;
    }

    public StudentEnrolments getStudentEnrolments(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, LusofonaApiException {
        StudentEnrolments response = executeAndSendMessage(
                requestBody,
                () -> lusofonaClient.getSIGESEnrolments(requestBody).getBody(),
                "Student enrolments fetched",
                "Error fetching student enrolments"
        );
        LusofonaResponseValidator.validateStudentEnrolments(response, "Get Student Enrolments");
        return response;
    }

    public StudentSchedule getUserScheduleSemester(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, LusofonaApiException {
        StudentSchedule response = executeAndSendMessage(
                requestBody,
                () -> {
                    Object responseBody = lusofonaClient.getUserScheduleSemester(requestBody).getBody();
                    if (responseBody instanceof pt.ulusofona.digital.wallet.domain.student.StudentSchedule) {
                        return (pt.ulusofona.digital.wallet.domain.student.StudentSchedule) responseBody;
                    } else if (responseBody instanceof java.util.Map) {
                        // Convert LinkedHashMap to StudentSchedule
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> responseMap = (java.util.Map<String, Object>) responseBody;
                        return convertMapToStudentSchedule(responseMap);
                    }
                    return null;
                },
                "User schedule fetched",
                "Error fetching user schedule"
        );
        LusofonaResponseValidator.validateStudentSchedule(response, "Get User Schedule");
        return response;
    }

    public StudentGrades getSIGESGrades(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, LusofonaApiException {
        StudentGrades response = executeAndSendMessage(
                requestBody,
                () -> lusofonaClient.getSIGESGrades(requestBody).getBody(),
                "Student grades fetched",
                "Error fetching student grades"
        );
        LusofonaResponseValidator.validateStudentGrades(response, "Get Student Grades");
        return response;
    }

    public Object login(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, LusofonaApiException {
        Object response = executeAndSendMessage(
                requestBody,
                () -> lusofonaClient.login(requestBody).getBody(),
                "Student login successful",
                "Error during login"
        );
        LusofonaResponseValidator.validateGenericResponse(response, "Student Login");
        return response;
    }

    public Object registration(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, LusofonaApiException {
        Object response = executeAndSendMessage(
                requestBody,
                () -> lusofonaClient.registration(requestBody).getBody(),
                "Student registration successful",
                "Error during registration"
        );
        LusofonaResponseValidator.validateGenericResponse(response, "Student Registration");
        return response;
    }

    public StudentEvals getSIGESStudentEvals(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, LusofonaApiException {
        StudentEvals response = executeAndSendMessage(
                requestBody,
                () -> lusofonaClient.getSIGESStudentEvals(requestBody).getBody(),
                "Student evaluations fetched",
                "Error fetching student evaluations"
        );
        LusofonaResponseValidator.validateStudentEvals(response, "Get Student Evaluations");
        return response;
    }

    private StudentSchedule convertMapToStudentSchedule(java.util.Map<String, Object> map) {
        try {
            // Extract data from the map
            @SuppressWarnings("unchecked")
            java.util.List<Object> listOfMonthsObj = (java.util.List<Object>) map.get("listOfMonths");
            Integer countObj = (Integer) map.get("count");
            @SuppressWarnings("unchecked")
            java.util.List<Object> scheduleObj = (java.util.List<Object>) map.get("schedule");
            String errorCode = (String) map.get("errorCode");
            
            // Convert listOfMonths
            java.util.List<pt.ulusofona.digital.wallet.domain.academic.MonthInfo> listOfMonths = new java.util.ArrayList<>();
            if (listOfMonthsObj != null) {
                for (Object monthObj : listOfMonthsObj) {
                    if (monthObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> monthMap = (java.util.Map<String, Object>) monthObj;
                        Object monthNumberObj = monthMap.get("monthNumber");
                        Object monthDateTimeObj = monthMap.get("month1stDatTime");
                        int occupationMonth = monthNumberObj instanceof Number ? ((Number) monthNumberObj).intValue() : 0;
                        long month1stDateTime = monthDateTimeObj instanceof Number ? ((Number) monthDateTimeObj).longValue() : 0L;
                        listOfMonths.add(new pt.ulusofona.digital.wallet.domain.academic.MonthInfo(
                            (String) monthMap.get("monthName"),
                            occupationMonth,
                            month1stDateTime
                        ));
                    }
                }
            }
            
            // Convert schedule entries
            java.util.List<pt.ulusofona.digital.wallet.domain.academic.ScheduleEntry> schedule = new java.util.ArrayList<>();
            if (scheduleObj != null) {
                for (Object scheduleEntryObj : scheduleObj) {
                    if (scheduleEntryObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> entryMap = (java.util.Map<String, Object>) scheduleEntryObj;
                        
                        // Extract class schedules
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> classSchedulesObj = (java.util.List<Object>) entryMap.get("schedule");
                        java.util.List<pt.ulusofona.digital.wallet.domain.academic.ClassSchedule> classSchedules = new java.util.ArrayList<>();
                        
                        if (classSchedulesObj != null) {
                            for (Object classScheduleObj : classSchedulesObj) {
                                if (classScheduleObj instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> classMap = (java.util.Map<String, Object>) classScheduleObj;
                                    classSchedules.add(new pt.ulusofona.digital.wallet.domain.academic.ClassSchedule(
                                        (String) classMap.get("curricularUnitName"),
                                        (String) classMap.get("className"),
                                        (String) classMap.get("startTime"),
                                        (String) classMap.get("inttendedDuration"),
                                        (String) classMap.get("roomName"),
                                        (String) classMap.get("teacherName"),
                                        (String) classMap.get("classType"),
                                        (Integer) classMap.get("classTypeCode")
                                    ));
                                }
                            }
                        }
                        
                        schedule.add(new pt.ulusofona.digital.wallet.domain.academic.ScheduleEntry(
                            ((Number) entryMap.get("occupationDate")).longValue(),
                            (Integer) entryMap.get("occupationMonth"),
                            (String) entryMap.get("dayOfWeek"),
                            (Integer) entryMap.get("dayOfWeekCode"),
                            (String) entryMap.get("scheduleType"),
                            classSchedules
                        ));
                    }
                }
            }
            
            int count = countObj != null ? countObj : 0;
            
            return new pt.ulusofona.digital.wallet.domain.student.StudentSchedule(
                listOfMonths,
                count,
                schedule,
                errorCode
            );
        } catch (Exception e) {
            logger.error("Error converting map to StudentSchedule: {}", e.getMessage(), e);
            // Return a basic schedule with error indication
            return new pt.ulusofona.digital.wallet.domain.student.StudentSchedule(
                java.util.Collections.emptyList(),
                0,
                java.util.Collections.emptyList(),
                "CONVERSION_ERROR"
            );
        }
    }
}