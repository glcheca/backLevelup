package com.levelup.levelup.reminder;

import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara el despacho de recordatorios vencidos cada `app.reminders.poll-interval-ms`. */
@Component
public class ReminderScheduler {

  private final Clock clock;
  private final ReminderDispatcher dispatcher;

  public ReminderScheduler(Clock clock, ReminderDispatcher dispatcher) {
    this.clock = clock;
    this.dispatcher = dispatcher;
  }

  @Scheduled(fixedDelayString = "${app.reminders.poll-interval-ms:30000}")
  public void tick() {
    dispatcher.dispatchDue(clock.instant());
  }
}
