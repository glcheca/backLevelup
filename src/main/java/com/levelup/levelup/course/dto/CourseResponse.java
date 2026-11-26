package com.levelup.levelup.course.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Curso.
 *
 * @param schedule bloques de horario; se rellena en el detalle y en `GET /schedule`, y va vacío en
 *     los listados para no cargar el horario de cada curso.
 */
public record CourseResponse(
    UUID id,
    String name,
    String code,
    String professor,
    String color,
    BigDecimal credits,
    String period,
    boolean archived,
    Instant createdAt,
    Instant updatedAt,
    List<ClassBlockResponse> schedule) {

  public static final List<ClassBlockResponse> NO_SCHEDULE = List.of();
}
