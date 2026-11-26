package com.levelup.levelup.course.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Actualización parcial: `null` significa «no cambiar». */
public record CourseUpdateRequest(
    @Size(min = 1, max = 80, message = "El nombre debe tener entre 1 y 80 caracteres") String name,
    @Size(max = 20, message = "El código no puede superar los 20 caracteres") String code,
    @Size(max = 120, message = "El profesor no puede superar los 120 caracteres") String professor,
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color debe ser hexadecimal, ej. #6366f1")
        String color,
    @DecimalMin(value = "0.0", message = "Los créditos no pueden ser negativos")
        @DecimalMax(value = "30.0", message = "Los créditos no pueden superar 30")
        @Digits(integer = 2, fraction = 1, message = "Los créditos admiten un decimal")
        BigDecimal credits,
    @Pattern(
            regexp = "^\\d{4}-[12]$",
            message = "El periodo debe tener el formato AAAA-N, ej. 2026-1")
        String period) {}
