package com.example.dcs.sis.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import com.example.dcs.sis.clients.SisClient;
import com.example.dcs.sis.domain.avro.UniversityRegistrationRequest;
import com.example.dcs.sis.domain.avro.UniversityServiceCommonRequest;
import com.example.dcs.sis.domain.student.*;
import com.example.dcs.sis.exception.ErrorCodes;
import com.example.dcs.sis.exception.ErrorUtils;
import com.example.dcs.sis.exception.ValidationException;
import com.example.dcs.sis.exception.http.*;
import com.example.dcs.sis.exception.sis.SisApiException;
import com.example.dcs.sis.exception.sis.SisResponseValidator;

import java.util.Map;
import java.util.function.Supplier;

@Service
public class SisService extends ServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(SisService.class);

    private final SisClient sisClient;
    private final ConversionService conversionService;

    public SisService(SisClient sisClient,
                           @Qualifier("mvcConversionService") ConversionService conversionService) { // Add @Qualifier here
        this.sisClient = sisClient;
        this.conversionService = conversionService;
    }

    private <T> T executeAndSendMessage(final Map<String, Object> requestBody,
                                        final Supplier<T> operation,
                                        final String successMessage,
                                        final String errorMessage) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, SisApiException {
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

    public StudentCourseCredits getStudentCourseCredits(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, SisApiException {
        StudentCourseCredits response = executeAndSendMessage(
                requestBody,
                () -> sisClient.getSIGESStudentCourseCredits(requestBody).getBody(),
                "Student course credits fetched",
                "Error fetching student course credits"
        );
        SisResponseValidator.validateStudentCourseCredits(response, "Get Student Course Credits");
        return response;
    }

    public StudentEnrolments getStudentEnrolments(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, SisApiException {
        StudentEnrolments response = executeAndSendMessage(
                requestBody,
                () -> sisClient.getSIGESEnrolments(requestBody).getBody(),
                "Student enrolments fetched",
                "Error fetching student enrolments"
        );
        SisResponseValidator.validateStudentEnrolments(response, "Get Student Enrolments");
        return response;
    }

    public StudentSchedule getUserScheduleSemester(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, SisApiException {
        StudentSchedule response = executeAndSendMessage(
                requestBody,
                () -> {
                    Object responseBody = sisClient.getUserScheduleSemester(requestBody).getBody();
                    if (responseBody instanceof com.example.dcs.sis.domain.student.StudentSchedule) {
                        return (com.example.dcs.sis.domain.student.StudentSchedule) responseBody;
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
        SisResponseValidator.validateStudentSchedule(response, "Get User Schedule");
        return response;
    }

    public StudentGrades getSIGESGrades(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, SisApiException {
        StudentGrades response = executeAndSendMessage(
                requestBody,
                () -> sisClient.getSIGESGrades(requestBody).getBody(),
                "Student grades fetched",
                "Error fetching student grades"
        );
        SisResponseValidator.validateStudentGrades(response, "Get Student Grades");
        return response;
    }

    public Object login(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, SisApiException {
        Object response = executeAndSendMessage(
                requestBody,
                () -> sisClient.login(requestBody).getBody(),
                "Student login successful",
                "Error during login"
        );
        SisResponseValidator.validateGenericResponse(response, "Student Login");
        return response;
    }

    public Object registration(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, SisApiException {
        Object response = executeAndSendMessage(
                requestBody,
                () -> sisClient.registration(requestBody).getBody(),
                "Student registration successful",
                "Error during registration"
        );
        SisResponseValidator.validateGenericResponse(response, "Student Registration");
        return response;
    }

    public StudentEvals getSIGESStudentEvals(final Map<String, Object> requestBody) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, BadRequestException, ForbiddenException, UnauthorizedException, NotFoundException, SisApiException {
        StudentEvals response = executeAndSendMessage(
                requestBody,
                () -> sisClient.getSIGESStudentEvals(requestBody).getBody(),
                "Student evaluations fetched",
                "Error fetching student evaluations"
        );
        SisResponseValidator.validateStudentEvals(response, "Get Student Evaluations");
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
            java.util.List<com.example.dcs.sis.domain.academic.MonthInfo> listOfMonths = new java.util.ArrayList<>();
            if (listOfMonthsObj != null) {
                for (Object monthObj : listOfMonthsObj) {
                    if (monthObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> monthMap = (java.util.Map<String, Object>) monthObj;
                        Object monthNumberObj = monthMap.get("monthNumber");
                        Object monthDateTimeObj = monthMap.get("month1stDatTime");
                        int occupationMonth = monthNumberObj instanceof Number ? ((Number) monthNumberObj).intValue() : 0;
                        long month1stDateTime = monthDateTimeObj instanceof Number ? ((Number) monthDateTimeObj).longValue() : 0L;
                        listOfMonths.add(new com.example.dcs.sis.domain.academic.MonthInfo(
                            (String) monthMap.get("monthName"),
                            occupationMonth,
                            month1stDateTime
                        ));
                    }
                }
            }
            
            // Convert schedule entries
            java.util.List<com.example.dcs.sis.domain.academic.ScheduleEntry> schedule = new java.util.ArrayList<>();
            if (scheduleObj != null) {
                for (Object scheduleEntryObj : scheduleObj) {
                    if (scheduleEntryObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> entryMap = (java.util.Map<String, Object>) scheduleEntryObj;
                        
                        // Extract class schedules
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> classSchedulesObj = (java.util.List<Object>) entryMap.get("schedule");
                        java.util.List<com.example.dcs.sis.domain.academic.ClassSchedule> classSchedules = new java.util.ArrayList<>();
                        
                        if (classSchedulesObj != null) {
                            for (Object classScheduleObj : classSchedulesObj) {
                                if (classScheduleObj instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> classMap = (java.util.Map<String, Object>) classScheduleObj;
                                    classSchedules.add(new com.example.dcs.sis.domain.academic.ClassSchedule(
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
                        
                        schedule.add(new com.example.dcs.sis.domain.academic.ScheduleEntry(
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
            
            return new com.example.dcs.sis.domain.student.StudentSchedule(
                listOfMonths,
                count,
                schedule,
                errorCode
            );
        } catch (Exception e) {
            logger.error("Error converting map to StudentSchedule: {}", e.getMessage(), e);
            // Return a basic schedule with error indication
            return new com.example.dcs.sis.domain.student.StudentSchedule(
                java.util.Collections.emptyList(),
                0,
                java.util.Collections.emptyList(),
                "CONVERSION_ERROR"
            );
        }
    }
}