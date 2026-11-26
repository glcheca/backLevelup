package com.levelup.levelup.reminder;

import java.time.Instant;
import java.util.UUID;

/**
 * Punto de extensión para los recordatorios (Etapa 3).
 *
 * <p>En la Etapa 1 solo se avisa de los cambios que podrían necesitar un recordatorio; la
 * implementación real (tabla `reminder` + job) llega en la Etapa 3 sin tocar eventos ni tareas.
 */
public interface ReminderHook {

  void onEventChanged(UUID userId, UUID eventId, Instant triggerAt);

  void onEventRemoved(UUID userId, UUID eventId);

  void onTaskChanged(UUID userId, UUID taskId, Instant dueAt);

  void onTaskRemoved(UUID userId, UUID taskId);
}
