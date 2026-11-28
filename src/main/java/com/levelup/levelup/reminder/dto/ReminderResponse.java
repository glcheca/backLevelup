package com.levelup.levelup.reminder.dto;

import com.levelup.levelup.reminder.ReminderStatus;
import com.levelup.levelup.reminder.ReminderTargetType;
import java.time.Instant;
import java.util.UUID;

public record ReminderResponse(
    UUID id,
    String title,
    String body,
    Instant triggerAt,
    ReminderStatus status,
    ReminderTargetType targetType,
    UUID targetId,
    String targetLabel,
    int attempts,
    Instant sentAt,
    Instant createdAt) {}
