package com.levelup.levelup.search;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.levelup.levelup.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SearchFlowTest extends ApiIntegrationTest {

  @Test
  void findsTheSameWordInEveryModuleAndIgnoresAccents() throws Exception {
    String token = registerAndGetToken();
    createCourse(token, "Matemáticas Discretas");
    createEvent(token, "Parcial de matemáticas");
    createTask(token, "Tarea de matemáticas");
    createNote(token, "Apuntes de matemáticas");

    mockMvc
        .perform(get("/api/v1/search?q=matematicas").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.courses.length()").value(1))
        .andExpect(jsonPath("$.events.length()").value(1))
        .andExpect(jsonPath("$.tasks.length()").value(1))
        .andExpect(jsonPath("$.notes.length()").value(1))
        .andExpect(jsonPath("$.courses[0].type").value("course"))
        .andExpect(jsonPath("$.notes[0].type").value("note"));

    mockMvc
        .perform(get("/api/v1/search?q=").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.courses.length()").value(0))
        .andExpect(jsonPath("$.notes.length()").value(0));

    mockMvc
        .perform(get("/api/v1/search?q=zzzz").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.events.length()").value(0));
  }

  @Test
  void limitsResultsAndIsolatesUsers() throws Exception {
    String token = registerAndGetToken();
    for (int index = 1; index <= 6; index++) {
      createCourse(token, "Curso " + index + " de álgebra");
    }

    mockMvc
        .perform(get("/api/v1/search?q=algebra").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.courses.length()").value(5));

    String stranger = registerAndGetToken();
    mockMvc
        .perform(get("/api/v1/search?q=algebra").header("Authorization", bearer(stranger)))
        .andExpect(jsonPath("$.courses.length()").value(0))
        .andExpect(jsonPath("$.events.length()").value(0))
        .andExpect(jsonPath("$.tasks.length()").value(0))
        .andExpect(jsonPath("$.notes.length()").value(0));
  }

  private void createCourse(String token, String name) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/courses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"period\":\"2026-2\"}".formatted(name)))
        .andExpect(status().isCreated());
  }

  private void createEvent(String token, String title) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/events")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"%s","type":"exam","startAt":"2027-08-01T10:00:00Z",
                     "endAt":"2027-08-01T12:00:00Z"}
                    """
                        .formatted(title)))
        .andExpect(status().isCreated());
  }

  private void createTask(String token, String title) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tasks")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"%s\"}".formatted(title)))
        .andExpect(status().isCreated());
  }

  private void createNote(String token, String title) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/notes")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"%s","contentMd":"Repaso de determinantes y matrices"}
                    """
                        .formatted(title)))
        .andExpect(status().isCreated());
  }
}
