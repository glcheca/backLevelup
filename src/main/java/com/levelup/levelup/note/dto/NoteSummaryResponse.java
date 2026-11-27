package com.levelup.levelup.note.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Apunte sin el contenido completo, para el listado maestro. */
public record NoteSummaryResponse(
    UUID id,
    String title,
    UUID courseId,
    String courseName,
    String courseColor,
    List<String> tags,
    boolean pinned,
    String excerpt,
    Instant createdAt,
    Instant updatedAt) {}
