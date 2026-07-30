package pt.ulusofona.student.exception;

import lombok.Getter;

/**
 * Base exception for Student Service errors
 */
@Getter
public class StudentServiceException extends RuntimeException {
    
    private final ErrorCodes errorCode;
    
    public StudentServiceException(ErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }
    
    public StudentServiceException(ErrorCodes errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public StudentServiceException(ErrorCodes errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public StudentServiceException(ErrorCodes errorCode, Throwable cause) {
        super(errorCode.getDescription(), cause);
        this.errorCode = errorCode;
    }
}

