package com.levelup.levelup.task.dto;

import com.levelup.levelup.task.Subtask;
import com.levelup.levelup.task.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    UUID courseId,
    String courseName,
    String courseColor,
    UUID eventId,
    TaskStatus status,
    int priority,
    Instant dueAt,
    Instant completedAt,
    boolean overdue,
    List<Subtask> subtasks,
    Instant createdAt,
    Instant updatedAt) {}
