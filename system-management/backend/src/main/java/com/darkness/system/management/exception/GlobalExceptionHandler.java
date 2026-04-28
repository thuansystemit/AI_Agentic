package com.darkness.system.management.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Spring Security's own AccessDeniedException (thrown by method security / filter chain)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ProblemDetail handleSpringAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Spring Security access denied: {}", ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ProblemDetail handleEmailExists(EmailAlreadyExistsException ex) {
        log.warn("Email already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DuplicateNameException.class)
    ProblemDetail handleDuplicateName(DuplicateNameException ex) {
        log.warn("Duplicate name: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    ProblemDetail handleLocked(AccountLockedException ex) {
        log.warn("Account locked until: {}", ex.getLockedUntil());
        ProblemDetail pd = problem(HttpStatus.FORBIDDEN, "Account is locked");
        pd.setProperty("lockedUntil", ex.getLockedUntil());
        return pd;
    }

    @ExceptionHandler(InvalidTokenException.class)
    ProblemDetail handleInvalidToken(InvalidTokenException ex) {
        log.warn("Invalid/expired token: {}", ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }

    @ExceptionHandler(CannotModifySelfException.class)
    ProblemDetail handleSelfModify(CannotModifySelfException ex) {
        log.warn("Self-modification attempt: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large. Maximum allowed size is 50 MB.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        log.warn("Validation failed: {}", errors);
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
