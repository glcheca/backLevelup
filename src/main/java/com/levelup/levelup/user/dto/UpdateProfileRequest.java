package com.levelup.levelup.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
        String name,
    @Size(max = 64, message = "La zona horaria no es válida") String timezone,
    @Pattern(regexp = "light|dark|system", message = "Tema inválido") String theme) {}
