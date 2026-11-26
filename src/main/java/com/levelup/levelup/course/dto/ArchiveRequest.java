package com.levelup.levelup.course.dto;

import jakarta.validation.constraints.NotNull;

public record ArchiveRequest(
    @NotNull(message = "Indica si el curso se archiva o no") Boolean archived) {}
