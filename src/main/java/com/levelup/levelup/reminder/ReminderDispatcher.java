package com.levelup.levelup.reminder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Toma los recordatorios vencidos y los procesa uno a uno. Cada uno se reclama con un `UPDATE`
 * condicional (`pending` → `sent`) dentro de su propia transacción, así que dos instancias nunca
 * crean la misma notificación: la segunda ve 0 filas afectadas.
 */
@Service
public class ReminderDispatcher {

  private static final int BATCH_SIZE = 100;

  private final ReminderRepository reminderRepository;
  private final ReminderProcessor processor;
  private final ReminderRetryService retryService;

  public ReminderDispatcher(
      ReminderRepository reminderRepository,
      ReminderProcessor processor,
      ReminderRetryService retryService) {
    this.reminderRepository = reminderRepository;
    this.processor = processor;
    this.retryService = retryService;
  }

  public void dispatchDue(Instant now) {
    List<UUID> due =
        reminderRepository.findDueIds(ReminderStatus.PENDING, now, PageRequest.of(0, BATCH_SIZE));
    for (UUID id : due) {
      try {
        processor.process(id, now);
      } catch (RuntimeException exception) {
        retryService.recordFailure(id, exception);
      }
    }
  }
}
