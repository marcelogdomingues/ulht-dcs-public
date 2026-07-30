package pt.ulusofona.digital.wallet.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for student codes.
 * Validates that student codes follow the expected format (e.g., a12345678).
 */
@Documented
@Constraint(validatedBy = StudentCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStudentCode {
    
    /**
     * Default error message when validation fails.
     */
    String message() default "Invalid student code format. Expected format: a[0-9]{8}";
    
    /**
     * Validation groups this constraint belongs to.
     */
    Class<?>[] groups() default {};
    
    /**
     * Additional payload information.
     */
    Class<? extends Payload>[] payload() default {};
}

