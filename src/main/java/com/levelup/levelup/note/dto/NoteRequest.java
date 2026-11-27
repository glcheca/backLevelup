package com.levelup.levelup.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Apunte. El `PATCH` recibe el objeto completo: `courseId` nulo desvincula el curso. */
public record NoteRequest(
    @NotBlank(message = "El título es obligatorio")
        @Size(max = 160, message = "El título no puede superar los 160 caracteres")
        String title,
    @Size(max = 200_000, message = "El apunte es demasiado largo") String contentMd,
    UUID courseId,
    @Size(max = 10, message = "Un apunte admite como máximo 10 etiquetas")
        List<
                @NotBlank(message = "Las etiquetas no pueden estar vacías")
                @Size(max = 40, message = "Cada etiqueta puede tener hasta 40 caracteres") String>
            tags,
    Boolean pinned) {}
