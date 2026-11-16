package com.catedra.catalogo.controller;

import com.catedra.catalogo.exception.IntegrationNotConfiguredException;
import com.catedra.catalogo.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Resource not found");
        problem.setProperty("code", "RESOURCE_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(IntegrationNotConfiguredException.class)
    public ProblemDetail handleNotConfigured(IntegrationNotConfiguredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setTitle("Integration not configured");
        problem.setProperty("code", "INTEGRATION_NOT_CONFIGURED");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La solicitud no es valida");
        problem.setTitle("Validation error");
        problem.setProperty("code", "VALIDATION_ERROR");

        List<Map<String, String>> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "objectName", error.getObjectName(),
                        "field", error.getField(),
                        "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalido"
                ))
                .toList();
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Validation error");
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Parametro invalido: " + exception.getName());
        problem.setTitle("Invalid parameter");
        problem.setProperty("code", "INVALID_PARAMETER");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Bad request");
        problem.setProperty("code", "BAD_REQUEST");
        return problem;
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleCatedraUnavailable(RestClientException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo comunicar con la catedra: " + exception.getMessage());
        problem.setTitle("Catedra unavailable");
        problem.setProperty("code", "CATEDRA_UNAVAILABLE");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno no esperado");
        problem.setTitle("Internal server error");
        problem.setProperty("code", "INTERNAL_ERROR");
        return problem;
    }
}
