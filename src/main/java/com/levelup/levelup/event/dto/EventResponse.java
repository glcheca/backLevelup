package com.levelup.levelup.event.dto;

import com.levelup.levelup.event.EventStatus;
import com.levelup.levelup.event.EventType;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
    UUID id,
    String title,
    String description,
    UUID courseId,
    String courseName,
    String courseColor,
    EventType type,
    Instant startAt,
    Instant endAt,
    boolean allDay,
    String location,
    EventStatus status,
    Integer reminderOffsetMinutes,
    Instant createdAt,
    Instant updatedAt) {}
