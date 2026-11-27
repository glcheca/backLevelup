package com.levelup.levelup.note.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteResponse(
    UUID id,
    String title,
    String contentMd,
    UUID courseId,
    String courseName,
    String courseColor,
    List<String> tags,
    boolean pinned,
    Instant createdAt,
    Instant updatedAt) {}
