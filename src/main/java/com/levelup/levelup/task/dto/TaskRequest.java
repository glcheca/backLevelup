package com.levelup.levelup.task.dto;

import com.levelup.levelup.task.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Tarea. Las actualizaciones envían el objeto completo: `courseId`/`eventId` nulos desvinculan. */
public record TaskRequest(
    @NotBlank(message = "El título es obligatorio")
        @Size(max = 160, message = "El título no puede superar los 160 caracteres")
        String title,
    @Size(max = 4000, message = "La descripción es demasiado larga") String description,
    UUID courseId,
    UUID eventId,
    TaskStatus status,
    @Min(value = 0, message = "La prioridad va de 0 a 2")
        @Max(value = 2, message = "La prioridad va de 0 a 2")
        Integer priority,
    Instant dueAt,
    @Size(max = 50, message = "Una tarea admite como máximo 50 subtareas") @Valid
        List<SubtaskRequest> subtasks,
    @Min(value = 0, message = "El aviso no puede ser negativo")
        @Max(value = 20160, message = "El aviso no puede superar los 14 días")
        Integer reminderOffsetMinutes) {}
