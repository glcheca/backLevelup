package com.levelup.levelup.grade.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
    UUID id,
    UUID courseId,
    String courseName,
    String courseColor,
    String name,
    BigDecimal weight,
    BigDecimal score,
    BigDecimal maxScore,
    BigDecimal percentage,
    Instant dueAt,
    String notes,
    Instant createdAt,
    Instant updatedAt) {}
