package com.levelup.levelup.common;

import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ProblemDetail handleApiException(ApiException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
    problem.setTitle(exception.getStatus().getReasonPhrase());
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los datos enviados.");
    problem.setTitle("Validación fallida");

    List<Map<String, String>> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error ->
                Map.of(
                    "field",
                    error.getField(),
                    "message",
                    error.getDefaultMessage() == null
                        ? "Valor inválido"
                        : error.getDefaultMessage()))
            .toList();
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "El recurso entra en conflicto con datos existentes.");
    problem.setTitle("Conflicto");
    return problem;
  }
}
