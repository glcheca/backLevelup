package com.levelup.levelup.reminder;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implementación de Etapa 1: solo registra la intención en el log. */
@Component
public class LoggingReminderHook implements ReminderHook {

  private static final Logger log = LoggerFactory.getLogger(LoggingReminderHook.class);

  @Override
  public void onEventChanged(UUID userId, UUID eventId, Instant triggerAt) {
    log.debug(
        "Recordatorio pendiente para el evento {} del usuario {} en {}",
        eventId,
        userId,
        triggerAt);
  }

  @Override
  public void onEventRemoved(UUID userId, UUID eventId) {
    log.debug("Cancelar recordatorios del evento {} del usuario {}", eventId, userId);
  }

  @Override
  public void onTaskChanged(UUID userId, UUID taskId, Instant dueAt) {
    log.debug(
        "Recordatorio pendiente para la tarea {} del usuario {} en {}", taskId, userId, dueAt);
  }

  @Override
  public void onTaskRemoved(UUID userId, UUID taskId) {
    log.debug("Cancelar recordatorios de la tarea {} del usuario {}", taskId, userId);
  }
}
