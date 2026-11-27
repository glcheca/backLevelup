package com.levelup.levelup.grade.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resumen de calificaciones de un curso.
 *
 * <p>`weightedAverage` es el promedio sobre las evaluaciones ya calificadas (0–100); `average20` lo
 * expresa en la escala 0–20 que usa la UI. `neededFor` es la nota (0–20) que hace falta en el peso
 * pendiente para alcanzar `target`.
 */
public record GradeSummaryResponse(
    UUID courseId,
    String courseName,
    String courseColor,
    BigDecimal weightedAverage,
    BigDecimal average20,
    BigDecimal totalWeight,
    BigDecimal totalWeightRegistered,
    BigDecimal totalWeightPending,
    boolean weightsValid,
    int gradedCount,
    int pendingCount,
    BigDecimal target,
    BigDecimal neededFor,
    boolean achievable,
    String message) {}
