package com.petmatch.community.controller.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.petmatch.community.exception.PetDeletionException;
import com.petmatch.community.exception.PetNotFoundException;
import com.petmatch.community.exception.SupportApplicationNotFoundException;
import com.petmatch.community.exception.SupportApplicationRuleException;
import com.petmatch.community.exception.SupportApplicationStateException;
import com.petmatch.community.exception.SupportRequestNotFoundException;
import com.petmatch.community.exception.SupportRequestStateException;

@RestControllerAdvice(basePackages = "com.petmatch.community.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler({
        PetNotFoundException.class,
        SupportRequestNotFoundException.class,
        SupportApplicationNotFoundException.class
    })
    ProblemDetail handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler({
        PetDeletionException.class,
        SupportRequestStateException.class,
        SupportApplicationRuleException.class,
        SupportApplicationStateException.class,
        DataIntegrityViolationException.class
    })
    ProblemDetail handleConflict(RuntimeException exception) {
        return problem(
            HttpStatus.CONFLICT,
            "Business rule conflict",
            "La operación no puede realizarse en el estado actual del recurso."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "Uno o más campos no son válidos."
        );

        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Invalid request body",
            "El cuerpo JSON no tiene el formato esperado."
        );
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
