package com.levelup.levelup.reminder;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra un fallo de envío en una transacción aparte (la de procesado se revierte). Tras {@value
 * #MAX_ATTEMPTS} intentos el recordatorio queda en `failed`.
 */
@Service
public class ReminderRetryService {

  static final int MAX_ATTEMPTS = 3;

  private static final Logger log = LoggerFactory.getLogger(ReminderRetryService.class);

  private final ReminderRepository reminderRepository;

  public ReminderRetryService(ReminderRepository reminderRepository) {
    this.reminderRepository = reminderRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(UUID reminderId, RuntimeException exception) {
    Reminder reminder = reminderRepository.findById(reminderId).orElse(null);
    if (reminder == null || reminder.getStatus() != ReminderStatus.PENDING) {
      return;
    }

    reminder.setAttempts((short) (reminder.getAttempts() + 1));
    if (reminder.getAttempts() >= MAX_ATTEMPTS) {
      reminder.setStatus(ReminderStatus.FAILED);
      log.error(
          "Recordatorio {} falló {} veces; se marca como failed: {}",
          reminderId,
          reminder.getAttempts(),
          exception.getMessage());
    } else {
      log.warn(
          "Recordatorio {} falló (intento {}): {}",
          reminderId,
          reminder.getAttempts(),
          exception.getMessage());
    }
    reminderRepository.save(reminder);
  }
}
