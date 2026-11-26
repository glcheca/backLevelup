package com.levelup.levelup.event.dto;

import com.levelup.levelup.event.EventStatus;
import com.levelup.levelup.event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento. En las actualizaciones se envía el objeto completo: `courseId` nulo desvincula el curso.
 */
public record EventRequest(
    @NotBlank(message = "El título es obligatorio")
        @Size(max = 160, message = "El título no puede superar los 160 caracteres")
        String title,
    @Size(max = 4000, message = "La descripción es demasiado larga") String description,
    UUID courseId,
    @NotNull(message = "El tipo es obligatorio") EventType type,
    @NotNull(message = "La fecha de inicio es obligatoria") Instant startAt,
    @NotNull(message = "La fecha de fin es obligatoria") Instant endAt,
    Boolean allDay,
    @Size(max = 160, message = "El lugar no puede superar los 160 caracteres") String location,
    EventStatus status) {}
