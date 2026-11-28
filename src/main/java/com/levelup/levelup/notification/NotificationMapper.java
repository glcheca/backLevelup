package com.levelup.levelup.notification;

import com.levelup.levelup.notification.dto.NotificationResponse;
import com.levelup.levelup.reminder.Reminder;

public final class NotificationMapper {

  private NotificationMapper() {}

  public static NotificationResponse toResponse(Notification notification) {
    Reminder reminder = notification.getReminder();
    return new NotificationResponse(
        notification.getId(),
        notification.getTitle(),
        notification.getBody(),
        notification.isRead(),
        notification.getReadAt(),
        notification.getCreatedAt(),
        reminder == null ? null : reminder.getTargetType(),
        reminder == null ? null : reminder.getTargetId());
  }
}
