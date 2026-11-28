package com.levelup.levelup.notification.dto;

import com.levelup.levelup.reminder.ReminderTargetType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String title,
    String body,
    boolean read,
    Instant readAt,
    Instant createdAt,
    ReminderTargetType targetType,
    UUID targetId) {}
