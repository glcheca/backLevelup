package com.levelup.levelup.reminder;

import java.time.Instant;
import java.util.UUID;

/**
 * Punto de extensión que usan eventos y tareas para mantener sus recordatorios.
 *
 * <p>`triggerAt == null` significa "no hay recordatorio" y cancela los pendientes del objetivo.
 */
public interface ReminderHook {

  void onEventChanged(
      UUID userId,
      UUID eventId,
      String title,
      String body,
      Instant triggerAt,
      Integer offsetMinutes);

  void onEventRemoved(UUID userId, UUID eventId);

  void onTaskChanged(
      UUID userId,
      UUID taskId,
      String title,
      String body,
      Instant triggerAt,
      Integer offsetMinutes);

  void onTaskRemoved(UUID userId, UUID taskId);
}
