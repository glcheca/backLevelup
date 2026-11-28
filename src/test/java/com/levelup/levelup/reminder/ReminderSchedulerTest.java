package com.levelup.levelup.reminder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.levelup.levelup.support.ApiIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ReminderSchedulerTest extends ApiIntegrationTest {

  @Autowired private ReminderDispatcher dispatcher;
  @Autowired private ReminderRetryService retryService;
  @Autowired private ReminderRepository reminderRepository;

  @Test
  void dispatchesDueRemindersOnceAndKeepsThemPerUser() throws Exception {
    String owner = registerAndGetToken();
    // Aviso ya vencido: el evento empieza en 30 min y el aviso es 60 min antes.
    createEvent(owner, "Vence pronto", Instant.now().plus(30, ChronoUnit.MINUTES));
    // Aviso futuro: el evento es dentro de 10 días.
    createEvent(owner, "Lejano", Instant.now().plus(10, ChronoUnit.DAYS));

    expectUnread(owner, 0);

    dispatcher.dispatchDue(Instant.now());
    expectUnread(owner, 1);

    // Un segundo despacho no duplica la notificación.
    dispatcher.dispatchDue(Instant.now());
    expectUnread(owner, 1);

    mockMvc
        .perform(get("/api/v1/notifications").header("Authorization", bearer(owner)))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].targetType").value("event"))
        .andExpect(jsonPath("$.content[0].title").value("Vence pronto"));

    // El recordatorio del otro usuario no aparece en su cuenta.
    String stranger = registerAndGetToken();
    expectUnread(stranger, 0);

    // Y un despacho posterior del mismo lote sigue sin disparar el futuro.
    dispatcher.dispatchDue(Instant.now().plus(1, ChronoUnit.HOURS));
    expectUnread(owner, 1);
  }

  @Test
  void marksReminderFailedAfterThreeAttempts() throws Exception {
    String token = registerAndGetToken();
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/events")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Con fallo","type":"exam","startAt":"%s","endAt":"%s",
                         "reminderOffsetMinutes":60}
                        """
                            .formatted(
                                Instant.now().plus(5, ChronoUnit.MINUTES),
                                Instant.now().plus(120, ChronoUnit.MINUTES))))
            .andExpect(status().isCreated())
            .andReturn();
    String eventId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    Reminder reminder =
        reminderRepository.findAll().stream()
            .filter(r -> eventId.equals(String.valueOf(r.getTargetId())))
            .findFirst()
            .orElseThrow();

    retryService.recordFailure(reminder.getId(), new RuntimeException("boom"));
    retryService.recordFailure(reminder.getId(), new RuntimeException("boom"));

    mockMvc
        .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(1));

    retryService.recordFailure(reminder.getId(), new RuntimeException("boom"));

    mockMvc
        .perform(get("/api/v1/reminders?status=failed").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].attempts").value(3));
  }

  private void expectUnread(String token, int expected) throws Exception {
    mockMvc
        .perform(get("/api/v1/notifications/unread-count").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.unread").value(expected));
  }

  private void createEvent(String token, String title, Instant startAt) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/events")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"%s","type":"exam","startAt":"%s","endAt":"%s",
                     "reminderOffsetMinutes":60}
                    """
                        .formatted(title, startAt, startAt.plus(2, ChronoUnit.HOURS))))
        .andExpect(status().isCreated());
  }
}
