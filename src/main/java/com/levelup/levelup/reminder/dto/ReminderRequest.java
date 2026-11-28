package com.levelup.levelup.reminder.dto;

import com.levelup.levelup.reminder.ReminderTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Recordatorio. `targetType` por defecto `custom` (sin entidad asociada). */
public record ReminderRequest(
    @NotBlank(message = "El título es obligatorio")
        @Size(max = 160, message = "El título no puede superar los 160 caracteres")
        String title,
    @Size(max = 4000, message = "El detalle es demasiado largo") String body,
    @NotNull(message = "La fecha del recordatorio es obligatoria") Instant triggerAt,
    ReminderTargetType targetType,
    UUID targetId) {}
