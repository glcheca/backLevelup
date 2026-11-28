package com.levelup.levelup.reminder;

import com.levelup.levelup.notification.Notification;
import com.levelup.levelup.notification.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Procesa un recordatorio en su propia transacción: reclamar + crear la notificación. */
@Service
public class ReminderProcessor {

  private static final Logger log = LoggerFactory.getLogger(ReminderProcessor.class);

  private final ReminderRepository reminderRepository;
  private final NotificationRepository notificationRepository;

  public ReminderProcessor(
      ReminderRepository reminderRepository, NotificationRepository notificationRepository) {
    this.reminderRepository = reminderRepository;
    this.notificationRepository = notificationRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void process(UUID reminderId, Instant now) {
    int claimed =
        reminderRepository.claim(reminderId, now, ReminderStatus.PENDING, ReminderStatus.SENT);
    if (claimed == 0) {
      return;
    }

    Reminder reminder = reminderRepository.findById(reminderId).orElseThrow();
    notificationRepository.save(
        new Notification(reminder.getUser(), reminder, reminder.getTitle(), reminder.getBody()));
    log.info("Recordatorio {} enviado al usuario {}", reminderId, reminder.getUser().getId());
  }
}
