package com.levelup.levelup.reminder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.levelup.levelup.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ReminderFlowTest extends ApiIntegrationTest {

  @Test
  void creatingAnEventWithOffsetCreatesAReminderAndMovingItUpdatesTheTrigger() throws Exception {
    String token = registerAndGetToken();

    String eventId =
        createEvent(token, "Examen de álgebra", "2027-03-10T15:00:00Z", "2027-03-10T17:00:00Z", 60);

    mockMvc
        .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].targetType").value("event"))
        .andExpect(jsonPath("$[0].targetId").value(eventId))
        .andExpect(jsonPath("$[0].status").value("pending"));
    assertPendingTrigger(token, "2027-03-10T14:00:00Z");

    mockMvc
        .perform(
            patch("/api/v1/events/" + eventId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    eventBody(
                        "Examen de álgebra", "2027-03-10T18:00:00Z", "2027-03-10T20:00:00Z", 60)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(1));
    assertPendingTrigger(token, "2027-03-10T17:00:00Z");
  }

  @Test
  void changingTheOffsetReplacesTheReminderWithoutDuplicates() throws Exception {
    String token = registerAndGetToken();
    String eventId =
        createEvent(token, "Parcial", "2027-04-01T10:00:00Z", "2027-04-01T12:00:00Z", 60);

    mockMvc
        .perform(
            patch("/api/v1/events/" + eventId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    eventBody("Parcial", "2027-04-01T10:00:00Z", "2027-04-01T12:00:00Z", 1440)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(1));
    assertPendingTrigger(token, "2027-03-31T10:00:00Z");

    mockMvc
        .perform(get("/api/v1/reminders?status=cancelled").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void disablingTheReminderOrDeletingTheEventCancelsIt() throws Exception {
    String token = registerAndGetToken();
    String eventId =
        createEvent(token, "Charla", "2027-05-05T14:00:00Z", "2027-05-05T15:00:00Z", 30);

    mockMvc
        .perform(
            patch("/api/v1/events/" + eventId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventBody("Charla", "2027-05-05T14:00:00Z", "2027-05-05T15:00:00Z", null)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(0));

    String other = createEvent(token, "Taller", "2027-05-06T14:00:00Z", "2027-05-06T15:00:00Z", 30);
    mockMvc
        .perform(delete("/api/v1/events/" + other).header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void completingATaskCancelsItsReminder() throws Exception {
    String token = registerAndGetToken();

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/tasks")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Informe","dueAt":"2027-06-01T23:00:00Z","priority":1,
                         "reminderOffsetMinutes":1440}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    String taskId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].targetType").value("task"));
    assertPendingTrigger(token, "2027-05-31T23:00:00Z");

    mockMvc
        .perform(
            patch("/api/v1/tasks/" + taskId + "/complete").header("Authorization", bearer(token)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void customRemindersAreOwnedAndValidated() throws Exception {
    String owner = registerAndGetToken();

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/reminders")
                    .header("Authorization", bearer(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Comprar calculadora","triggerAt":"2027-03-01T12:00:00Z"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.targetType").value("custom"))
            .andExpect(jsonPath("$.status").value("pending"))
            .andReturn();
    String reminderId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    String stranger = registerAndGetToken();
    mockMvc
        .perform(get("/api/v1/reminders").header("Authorization", bearer(stranger)))
        .andExpect(jsonPath("$.length()").value(0));
    mockMvc
        .perform(
            delete("/api/v1/reminders/" + reminderId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(delete("/api/v1/reminders/" + reminderId).header("Authorization", bearer(owner)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/api/v1/reminders")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \",\"triggerAt\":\"2027-03-01T12:00:00Z\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/reminders")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Sin evento\",\"triggerAt\":\"2027-03-01T12:00:00Z\",\"targetType\":\"event\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cannotLinkAReminderToAnotherUsersEvent() throws Exception {
    String owner = registerAndGetToken();
    String eventId =
        createEvent(owner, "Privado", "2027-07-01T10:00:00Z", "2027-07-01T11:00:00Z", 60);

    String stranger = registerAndGetToken();
    mockMvc
        .perform(
            post("/api/v1/reminders")
                .header("Authorization", bearer(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Intruso","triggerAt":"2027-06-30T10:00:00Z","targetType":"event","targetId":"%s"}
                    """
                        .formatted(eventId)))
        .andExpect(status().isNotFound());
  }

  private String createEvent(
      String token, String title, String startAt, String endAt, Integer offsetMinutes)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/events")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(eventBody(title, startAt, endAt, offsetMinutes)))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  /** Comprueba que hay un único recordatorio pendiente con el `triggerAt` esperado. */
  private void assertPendingTrigger(String token, String expected) throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/v1/reminders?status=pending").header("Authorization", bearer(token)))
            .andExpect(jsonPath("$.length()").value(1))
            .andReturn();
    java.util.List<String> triggers =
        JsonPath.read(result.getResponse().getContentAsString(), "$[*].triggerAt");
    org.junit.jupiter.api.Assertions.assertEquals(
        java.time.Instant.parse(expected), java.time.Instant.parse(triggers.get(0)));
  }

  private String eventBody(String title, String startAt, String endAt, Integer offsetMinutes) {
    return """
    {"title":"%s","type":"exam","startAt":"%s","endAt":"%s","reminderOffsetMinutes":%s}
    """
        .formatted(title, startAt, endAt, offsetMinutes == null ? "null" : offsetMinutes);
  }
}
