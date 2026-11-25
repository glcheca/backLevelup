package com.levelup.levelup.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo válido") @Size(max = 255) String email,

		@NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,

		@NotBlank(message = "El nombre es obligatorio") @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres") String name,

		@Size(max = 64) String timezone) {
}
