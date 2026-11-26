package com.levelup.levelup.course;

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

class CourseFlowTest extends ApiIntegrationTest {

  @Test
  void createsAndListsOwnCourses() throws Exception {
    String token = registerAndGetToken();
    createCourse(token, "Álgebra", "2026-1");

    mockMvc
        .perform(get("/api/v1/courses").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Álgebra"))
        .andExpect(jsonPath("$.content[0].color").value("#22c55e"))
        .andExpect(jsonPath("$.content[0].schedule").isEmpty());
  }

  @Test
  void rejectsDuplicateNameAndPeriod() throws Exception {
    String token = registerAndGetToken();
    createCourse(token, "Física", "2026-1");
    createCourse(token, "Física", "2026-2");

    mockMvc
        .perform(
            post("/api/v1/courses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseBody("Física", "2026-1")))
        .andExpect(status().isConflict());
  }

  @Test
  void rejectsInvalidColorAndCredits() throws Exception {
    String token = registerAndGetToken();

    mockMvc
        .perform(
            post("/api/v1/courses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseBody("Química", "2026-1").replace("#22c55e", "verde")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("color"));

    mockMvc
        .perform(
            post("/api/v1/courses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseBody("Química", "2026-1").replace("4.5", "44")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("credits"));
  }

  @Test
  void anotherUserCannotSeeOrModifyCourses() throws Exception {
    String owner = registerAndGetToken();
    String courseId = createCourse(owner, "Historia", "2026-1");

    String stranger = registerAndGetToken();

    mockMvc
        .perform(get("/api/v1/courses").header("Authorization", bearer(stranger)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));

    mockMvc
        .perform(get("/api/v1/courses/" + courseId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            patch("/api/v1/courses/" + courseId)
                .header("Authorization", bearer(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Robado\"}"))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(delete("/api/v1/courses/" + courseId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/schedule")
                .header("Authorization", bearer(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(1, "08:00", "10:00")))
        .andExpect(status().isNotFound());
  }

  @Test
  void archiveMovesCourseOutOfTheDefaultList() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Ética", "2026-1");

    mockMvc
        .perform(
            patch("/api/v1/courses/" + courseId + "/archive")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"archived\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archived").value(true));

    mockMvc
        .perform(get("/api/v1/courses?archived=false").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(0));

    mockMvc
        .perform(get("/api/v1/courses?archived=true").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(get("/api/v1/courses").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void updateChangesColorAndRejectsDuplicatedIdentity() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Arte", "2026-1");
    createCourse(token, "Música", "2026-1");

    mockMvc
        .perform(
            patch("/api/v1/courses/" + courseId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"#ef4444\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.color").value("#ef4444"))
        .andExpect(jsonPath("$.name").value("Arte"));

    mockMvc
        .perform(
            patch("/api/v1/courses/" + courseId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Música\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void deleteRemovesTheCourseAndItsSchedule() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Redes", "2026-1");
    createBlock(token, courseId, 2, "10:00", "12:00");

    mockMvc
        .perform(delete("/api/v1/courses/" + courseId).header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/courses/" + courseId).header("Authorization", bearer(token)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/v1/schedule").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void scheduleRequiresEndAfterStart() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Cálculo", "2026-1");

    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/schedule")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(3, "12:00", "10:00")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void scheduleWarnsAboutOverlapsButStillCreatesTheBlock() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Programación", "2026-1");
    createBlock(token, courseId, 1, "08:00", "10:00");

    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/schedule")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(1, "09:00", "11:00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.warning").isNotEmpty())
        .andExpect(jsonPath("$.dayOfWeek").value(1));

    mockMvc
        .perform(get("/api/v1/schedule").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].courseName").value("Programación"));
  }

  @Test
  void scheduleDetailIsReturnedWithTheCourse() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Bases de datos", "2026-1");
    createBlock(token, courseId, 4, "16:00", "18:00");

    mockMvc
        .perform(get("/api/v1/courses/" + courseId).header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule.length()").value(1))
        .andExpect(jsonPath("$.schedule[0].startTime").value("16:00:00"))
        .andExpect(jsonPath("$.schedule[0].room").value("Aula 302"));
  }

  @Test
  void courseRoutesRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/courses")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/schedule")).andExpect(status().isUnauthorized());
  }

  private String createCourse(String token, String name, String period) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/courses")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(courseBody(name, period)))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private void createBlock(String token, String courseId, int day, String start, String end)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/schedule")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(day, start, end)))
        .andExpect(status().isCreated());
  }

  private String courseBody(String name, String period) {
    return """
    {"name":"%s","period":"%s","color":"#22c55e","professor":"Prof. X","credits":4.5,"code":"MAT101"}
    """
        .formatted(name, period);
  }

  private String blockBody(int day, String start, String end) {
    return """
    {"dayOfWeek":%d,"startTime":"%s","endTime":"%s","room":"Aula 302"}
    """
        .formatted(day, start, end);
  }
}
