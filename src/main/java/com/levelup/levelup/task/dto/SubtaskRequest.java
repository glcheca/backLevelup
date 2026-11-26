package com.levelup.levelup.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubtaskRequest(
    @Size(max = 40, message = "El identificador de la subtarea es demasiado largo") String id,
    @NotBlank(message = "La subtarea necesita un título")
        @Size(max = 120, message = "La subtarea no puede superar los 120 caracteres")
        String title,
    Boolean done) {}
