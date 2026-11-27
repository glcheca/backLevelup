package com.levelup.levelup.note.dto;

import jakarta.validation.constraints.NotNull;

public record NotePinRequest(
    @NotNull(message = "Indica si el apunte queda fijado") Boolean pinned) {}
