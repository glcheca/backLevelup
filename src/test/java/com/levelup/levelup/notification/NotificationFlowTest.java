package com.levelup.levelup.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.levelup.levelup.reminder.ReminderDispatcher;
import com.levelup.levelup.support.ApiIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class NotificationFlowTest extends ApiIntegrationTest {

  @Autowired private ReminderDispatcher dispatcher;

  @Test
  void listsMarksAndCountsNotifications() throws Exception {
    String token = registerAndGetToken();
    createDueEvent(token, "Parcial de álgebra");
    createDueEvent(token, "Entrega de laboratorio");
    dispatcher.dispatchDue(Instant.now());

    mockMvc
        .perform(get("/api/v1/notifications/unread-count").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.unread").value(2));

    MvcResult list =
        mockMvc
            .perform(get("/api/v1/notifications").header("Authorization", bearer(token)))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].read").value(false))
            .andReturn();
    String notificationId =
        JsonPath.read(list.getResponse().getContentAsString(), "$.content[0].id");

    mockMvc
        .perform(
            patch("/api/v1/notifications/" + notificationId + "/read")
                .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.read").value(true));

    mockMvc
        .perform(get("/api/v1/notifications/unread-count").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.unread").value(1));

    mockMvc
        .perform(
            get("/api/v1/notifications?unreadOnly=true").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(patch("/api/v1/notifications/read-all").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.unread").value(0));

    mockMvc
        .perform(
            get("/api/v1/notifications?unreadOnly=true").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(0));

    mockMvc
        .perform(get("/api/v1/notifications").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void notificationsBelongToOneUser() throws Exception {
    String owner = registerAndGetToken();
    createDueEvent(owner, "Solo para mí");
    dispatcher.dispatchDue(Instant.now());

    MvcResult list =
        mockMvc
            .perform(get("/api/v1/notifications").header("Authorization", bearer(owner)))
            .andReturn();
    String notificationId =
        JsonPath.read(list.getResponse().getContentAsString(), "$.content[0].id");

    String stranger = registerAndGetToken();
    mockMvc
        .perform(get("/api/v1/notifications").header("Authorization", bearer(stranger)))
        .andExpect(jsonPath("$.totalElements").value(0));
    mockMvc
        .perform(
            get("/api/v1/notifications/unread-count").header("Authorization", bearer(stranger)))
        .andExpect(jsonPath("$.unread").value(0));
    mockMvc
        .perform(
            patch("/api/v1/notifications/" + notificationId + "/read")
                .header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());
  }

  private void createDueEvent(String token, String title) throws Exception {
    Instant start = Instant.now().plus(30, ChronoUnit.MINUTES);
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
                        .formatted(title, start, start.plus(2, ChronoUnit.HOURS))))
        .andExpect(status().isCreated());
  }
}
