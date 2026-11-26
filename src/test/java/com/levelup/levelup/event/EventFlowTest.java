package com.levelup.levelup.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.levelup.levelup.support.ApiIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class EventFlowTest extends ApiIntegrationTest {

  @Test
  void filtersEventsByOverlappingRange() throws Exception {
    String token = registerAndGetToken();
    createEvent(
        token, "Parcial de Álgebra", "2026-03-10T14:00:00Z", "2026-03-10T16:00:00Z", "exam");
    createEvent(
        token,
        "Entrega de laboratorio",
        "2026-03-20T14:00:00Z",
        "2026-03-20T16:00:00Z",
        "assignment");

    mockMvc
        .perform(
            get("/api/v1/events?from=2026-03-01T00:00:00Z&to=2026-03-15T00:00:00Z")
                .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Parcial de Álgebra"));

    mockMvc
        .perform(
            get("/api/v1/events?from=2026-03-01T00:00:00Z&to=2026-04-01T00:00:00Z")
                .header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void filtersByTypeAndCourse() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token);
    createEventWithCourse(
        token, "Parcial", "2026-03-10T14:00:00Z", "2026-03-10T16:00:00Z", "exam", courseId);
    createEvent(token, "Cita personal", "2026-03-11T14:00:00Z", "2026-03-11T15:00:00Z", "personal");

    mockMvc
        .perform(get("/api/v1/events?type=exam").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].courseId").value(courseId))
        .andExpect(jsonPath("$.content[0].courseColor").value("#22c55e"));

    mockMvc
        .perform(get("/api/v1/events?courseId=" + courseId).header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void allDayEventIsNormalisedToTheUserTimezone() throws Exception {
    String token = registerAndGetToken();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/events")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Feriado","type":"personal","startAt":"2026-03-14T18:00:00Z",
                         "endAt":"2026-03-14T18:00:00Z","allDay":true}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.allDay").value(true))
            .andReturn();

    String body = result.getResponse().getContentAsString();
    // America/Lima es UTC-5: el día local 2026-03-14 va de 05:00Z a 04:59:59Z del día siguiente.
    assertEquals(
        Instant.parse("2026-03-14T05:00:00Z"), Instant.parse(JsonPath.read(body, "$.startAt")));
    assertEquals(
        Instant.parse("2026-03-15T04:59:59Z"), Instant.parse(JsonPath.read(body, "$.endAt")));
  }

  @Test
  void rejectsEndBeforeStart() throws Exception {
    String token = registerAndGetToken();

    mockMvc
        .perform(
            post("/api/v1/events")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    eventBody(
                        "Mal rango", "2026-03-10T18:00:00Z", "2026-03-10T16:00:00Z", "personal")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUnknownTypeAndForeignCourse() throws Exception {
    String token = registerAndGetToken();

    mockMvc
        .perform(
            post("/api/v1/events")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    eventBody("Raro", "2026-03-10T14:00:00Z", "2026-03-10T16:00:00Z", "fiesta")))
        .andExpect(status().isBadRequest());

    String stranger = registerAndGetToken();
    String foreignCourse = createCourse(stranger);

    mockMvc
        .perform(
            post("/api/v1/events")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    eventBodyWithCourse(
                        "Ajeno",
                        "2026-03-10T14:00:00Z",
                        "2026-03-10T16:00:00Z",
                        "exam",
                        foreignCourse)))
        .andExpect(status().isNotFound());
  }

  @Test
  void anotherUserCannotReadEvents() throws Exception {
    String owner = registerAndGetToken();
    String eventId =
        createEvent(owner, "Privado", "2026-03-10T14:00:00Z", "2026-03-10T16:00:00Z", "personal");

    String stranger = registerAndGetToken();

    mockMvc
        .perform(get("/api/v1/events").header("Authorization", bearer(stranger)))
        .andExpect(jsonPath("$.totalElements").value(0));

    mockMvc
        .perform(get("/api/v1/events/" + eventId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(delete("/api/v1/events/" + eventId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/v1/events/" + eventId).header("Authorization", bearer(owner)))
        .andExpect(status().isOk());
  }

  private String createEvent(String token, String title, String startAt, String endAt, String type)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/events")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(eventBody(title, startAt, endAt, type)))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private void createEventWithCourse(
      String token, String title, String startAt, String endAt, String type, String courseId)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/events")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventBodyWithCourse(title, startAt, endAt, type, courseId)))
        .andExpect(status().isCreated());
  }

  private String createCourse(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/courses")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Álgebra","period":"2026-1","color":"#22c55e"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private String eventBody(String title, String startAt, String endAt, String type) {
    return """
    {"title":"%s","type":"%s","startAt":"%s","endAt":"%s","description":"detalle"}
    """
        .formatted(title, type, startAt, endAt);
  }

  private String eventBodyWithCourse(
      String title, String startAt, String endAt, String type, String courseId) {
    return """
    {"title":"%s","type":"%s","startAt":"%s","endAt":"%s","courseId":"%s"}
    """
        .formatted(title, type, startAt, endAt, courseId);
  }
}
