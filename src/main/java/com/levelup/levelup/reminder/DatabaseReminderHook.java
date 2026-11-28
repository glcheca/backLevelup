package com.levelup.levelup.reminder;

import com.levelup.levelup.user.UserService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación real: mantiene una fila por recordatorio. La clave de idempotencia
 * (`targetType:targetId:offset`) permite recrear un recordatorio cancelado sin duplicar.
 */
@Component
public class DatabaseReminderHook implements ReminderHook {

  private static final Logger log = LoggerFactory.getLogger(DatabaseReminderHook.class);

  private final ReminderRepository reminderRepository;
  private final UserService userService;

  public DatabaseReminderHook(ReminderRepository reminderRepository, UserService userService) {
    this.reminderRepository = reminderRepository;
    this.userService = userService;
  }

  @Override
  public void onEventChanged(
      UUID userId,
      UUID eventId,
      String title,
      String body,
      Instant triggerAt,
      Integer offsetMinutes) {
    apply(userId, ReminderTargetType.EVENT, eventId, title, body, triggerAt, offsetMinutes);
  }

  @Override
  public void onEventRemoved(UUID userId, UUID eventId) {
    cancel(ReminderTargetType.EVENT, eventId);
  }

  @Override
  public void onTaskChanged(
      UUID userId,
      UUID taskId,
      String title,
      String body,
      Instant triggerAt,
      Integer offsetMinutes) {
    apply(userId, ReminderTargetType.TASK, taskId, title, body, triggerAt, offsetMinutes);
  }

  @Override
  public void onTaskRemoved(UUID userId, UUID taskId) {
    cancel(ReminderTargetType.TASK, taskId);
  }

  private void apply(
      UUID userId,
      ReminderTargetType targetType,
      UUID targetId,
      String title,
      String body,
      Instant triggerAt,
      Integer offsetMinutes) {
    cancel(targetType, targetId);
    if (triggerAt == null) {
      log.debug("Sin recordatorio para {} {}", targetType.value(), targetId);
      return;
    }

    String key =
        "%s:%s:%s"
            .formatted(
                targetType.value(), targetId, offsetMinutes == null ? "auto" : offsetMinutes);
    Reminder reminder =
        reminderRepository
            .findByIdempotencyKey(key)
            .orElseGet(
                () -> new Reminder(userService.requireUser(userId), targetType, targetId, key));
    reminder.setTitle(title);
    reminder.setBody(body);
    reminder.setTriggerAt(triggerAt);
    reminder.setStatus(ReminderStatus.PENDING);
    reminder.setSentAt(null);
    reminder.setAttempts((short) 0);
    reminderRepository.save(reminder);
  }

  private void cancel(ReminderTargetType targetType, UUID targetId) {
    reminderRepository.cancelPendingForTarget(
        targetType, targetId, ReminderStatus.PENDING, ReminderStatus.CANCELLED);
  }
}
