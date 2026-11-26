package com.levelup.levelup.common;

import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "El cuerpo de la petición no tiene el formato esperado.");
    problem.setTitle("Solicitud inválida");
    return problem;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "El parámetro '%s' tiene un valor inválido.".formatted(exception.getName()));
    problem.setTitle("Solicitud inválida");
    return problem;
  }
}
