package com.levelup.levelup.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record ClassBlockRequest(
    @NotNull(message = "El día es obligatorio") @Min(1) @Max(7) Integer dayOfWeek,
    @NotNull(message = "La hora de inicio es obligatoria") LocalTime startTime,
    @NotNull(message = "La hora de fin es obligatoria") LocalTime endTime,
    @Size(max = 60, message = "El aula no puede superar los 60 caracteres") String room) {}
