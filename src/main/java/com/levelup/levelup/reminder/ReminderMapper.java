package com.levelup.levelup.reminder;

import com.levelup.levelup.reminder.dto.ReminderResponse;

public final class ReminderMapper {

  private ReminderMapper() {}

  public static ReminderResponse toResponse(Reminder reminder, String targetLabel) {
    return new ReminderResponse(
        reminder.getId(),
        reminder.getTitle(),
        reminder.getBody(),
        reminder.getTriggerAt(),
        reminder.getStatus(),
        reminder.getTargetType(),
        reminder.getTargetId(),
        targetLabel,
        reminder.getAttempts(),
        reminder.getSentAt(),
        reminder.getCreatedAt());
  }
}
