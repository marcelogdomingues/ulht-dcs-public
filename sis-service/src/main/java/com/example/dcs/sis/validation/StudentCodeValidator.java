package com.example.dcs.sis.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Validator implementation for student code format validation.
 * Validates that student codes follow the pattern: a[0-9]{8}
 * Example valid codes: a12345678, a12345678
 */
public class StudentCodeValidator implements ConstraintValidator<ValidStudentCode, String> {
    
    /**
     * Regular expression pattern for valid student codes.
     * Format: lowercase 'a' followed by exactly 8 digits.
     */
    private static final Pattern STUDENT_CODE_PATTERN = Pattern.compile("^a\\d{8}$");
    
    @Override
    public void initialize(ValidStudentCode constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String studentCode, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull annotation
        if (!StringUtils.hasText(studentCode)) {
            return true;
        }
        
        // Check if the student code matches the expected pattern
        boolean isValid = STUDENT_CODE_PATTERN.matcher(studentCode.trim()).matches();
        
        // If validation fails, customize the error message
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Student code '%s' is invalid. Expected format: a[0-9]{8} (e.g., a12345678)", studentCode)
            ).addConstraintViolation();
        }
        
        return isValid;
    }
}

