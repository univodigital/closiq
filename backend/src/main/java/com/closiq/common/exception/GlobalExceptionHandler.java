package com.closiq.common.exception;

import com.closiq.common.web.ProblemDetailResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClosiqException.class)
    public ResponseEntity<ProblemDetailResponse> handleClosiqException(
            ClosiqException ex, HttpServletRequest request) {

        return buildProblem(ex.getStatus(), ex.getErrorCode(), ex.getDetail(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ProblemDetailResponse.FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ProblemDetailResponse.FieldErrorItem.builder()
                        .field(fieldError.getField())
                        .code("VALIDATION_ERROR")
                        .message(fieldError.getDefaultMessage())
                        .rejectedValue(fieldError.getRejectedValue())
                        .build())
                .toList();

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.getDefaultDetail(),
                request,
                errors);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetailResponse> handleBindException(
            BindException ex, HttpServletRequest request) {

        List<ProblemDetailResponse.FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ProblemDetailResponse.FieldErrorItem.builder()
                        .field(fieldError.getField())
                        .code("VALIDATION_ERROR")
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.getDefaultDetail(),
                request,
                errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetailResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ProblemDetailResponse.FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(violation -> ProblemDetailResponse.FieldErrorItem.builder()
                        .field(violation.getPropertyPath().toString())
                        .code("VALIDATION_ERROR")
                        .message(violation.getMessage())
                        .build())
                .toList();

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.getDefaultDetail(),
                request,
                errors);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetailResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {

        return buildProblem(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED.getDefaultDetail(),
                request,
                null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetailResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        return buildProblem(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                ErrorCode.FORBIDDEN.getDefaultDetail(),
                request,
                null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetailResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        return buildProblem(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                ex.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);

        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.getDefaultDetail(),
                request,
                null);
    }

    private ResponseEntity<ProblemDetailResponse> buildProblem(
            HttpStatus status,
            ErrorCode errorCode,
            String detail,
            HttpServletRequest request,
            List<ProblemDetailResponse.FieldErrorItem> errors) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type("https://api.closiq.com/errors/" + errorCode.name().toLowerCase().replace('_', '-'))
                .title(errorCode.name().replace('_', ' '))
                .status(status.value())
                .code(errorCode.name())
                .detail(detail)
                .instance(request.getRequestURI())
                .requestId(ClosiqRequestIdFilter.getRequestId(request))
                .timestamp(Instant.now())
                .errors(errors)
                .build();

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
