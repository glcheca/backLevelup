package com.levelup.levelup.grade.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Evaluación. El `PATCH` recibe el objeto completo. */
public record GradeRequest(
    @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,
    @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "0.01", message = "El peso debe ser mayor que 0")
        @DecimalMax(value = "100", message = "El peso no puede superar 100")
        BigDecimal weight,
    @DecimalMin(value = "0", message = "La nota no puede ser negativa") BigDecimal score,
    @NotNull(message = "La nota máxima es obligatoria")
        @DecimalMin(value = "0.01", message = "La nota máxima debe ser mayor que 0")
        BigDecimal maxScore,
    Instant dueAt,
    @Size(max = 4000, message = "Las notas son demasiado largas") String notes) {}
