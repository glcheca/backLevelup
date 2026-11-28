package com.levelup.levelup.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Verifica que el job usa el `Clock` inyectado (prueba determinista con `Clock.fixed`). */
class ReminderSchedulerUnitTest {

  @Test
  void dispatchesWithTheInjectedClock() {
    Instant fixed = Instant.parse("2030-01-01T10:00:00Z");
    CapturingDispatcher dispatcher = new CapturingDispatcher();
    ReminderScheduler scheduler =
        new ReminderScheduler(Clock.fixed(fixed, ZoneOffset.UTC), dispatcher);

    scheduler.tick();
    scheduler.tick();

    assertEquals(fixed, dispatcher.lastNow);
    assertEquals(2, dispatcher.calls);
  }

  private static final class CapturingDispatcher extends ReminderDispatcher {

    private Instant lastNow;
    private int calls;

    private CapturingDispatcher() {
      super(null, null, null);
    }

    @Override
    public void dispatchDue(Instant now) {
      this.lastNow = now;
      this.calls++;
    }
  }
}
