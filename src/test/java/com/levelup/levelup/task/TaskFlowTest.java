package com.levelup.levelup.task;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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

class TaskFlowTest extends ApiIntegrationTest {

  @Test
  void createsTaskWithSubtasksAndReadsItBack() throws Exception {
    String token = registerAndGetToken();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/tasks")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(taskBody("Informe de laboratorio", "2027-03-12T23:00:00Z", "todo", 2)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subtasks.length()").value(2))
            .andExpect(jsonPath("$.subtasks[0].title").value("Recoger datos"))
            .andExpect(jsonPath("$.subtasks[0].done").value(true))
            .andExpect(jsonPath("$.subtasks[0].id").isNotEmpty())
            .andExpect(jsonPath("$.status").value("todo"))
            .andExpect(jsonPath("$.priority").value(2))
            .andExpect(jsonPath("$.overdue").value(false))
            .andReturn();

    String taskId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(get("/api/v1/tasks/" + taskId).header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subtasks[1].done").value(false));
  }

  @Test
  void rejectsInvalidSubtasks() throws Exception {
    String token = registerAndGetToken();

    mockMvc
        .perform(
            post("/api/v1/tasks")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Con subtarea vacía","subtasks":[{"title":"   "}]}
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/tasks")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Con ids repetidos","subtasks":[
                      {"id":"s1","title":"Uno"},{"id":"s1","title":"Dos"}]}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void completeAndReopenManageCompletedAt() throws Exception {
    String token = registerAndGetToken();
    String taskId = createTask(token, "Ensayo", "2027-03-12T23:00:00Z", "todo", 1);

    MvcResult completed =
        mockMvc
            .perform(
                patch("/api/v1/tasks/" + taskId + "/complete")
                    .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("done"))
            .andReturn();
    assertNotNull(JsonPath.read(completed.getResponse().getContentAsString(), "$.completedAt"));

    mockMvc
        .perform(
            patch("/api/v1/tasks/" + taskId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskBody("Ensayo", "2027-03-12T23:00:00Z", "todo", 1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("todo"))
        .andExpect(jsonPath("$.completedAt").doesNotExist());
  }

  @Test
  void filtersByStatusCourseAndDueDate() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token);
    createTaskInCourse(token, "Tarea del curso", "2027-03-12T23:00:00Z", "todo", 1, courseId);
    createTask(token, "Tarea suelta", "2027-04-01T23:00:00Z", "todo", 0);
    String done = createTask(token, "Ya hecha", "2027-03-12T23:00:00Z", "todo", 1);
    mockMvc
        .perform(
            patch("/api/v1/tasks/" + done + "/complete").header("Authorization", bearer(token)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/tasks?status=todo").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(2));

    mockMvc
        .perform(get("/api/v1/tasks?status=done").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(get("/api/v1/tasks?courseId=" + courseId).header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(
            get("/api/v1/tasks?status=todo&dueBefore=2027-03-20T00:00:00Z")
                .header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Tarea del curso"));
  }

  @Test
  void anotherUserCannotTouchTasks() throws Exception {
    String owner = registerAndGetToken();
    String taskId = createTask(owner, "Privada", "2027-03-12T23:00:00Z", "todo", 1);

    String stranger = registerAndGetToken();

    mockMvc
        .perform(get("/api/v1/tasks").header("Authorization", bearer(stranger)))
        .andExpect(jsonPath("$.totalElements").value(0));

    mockMvc
        .perform(
            patch("/api/v1/tasks/" + taskId + "/complete")
                .header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(delete("/api/v1/tasks/" + taskId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());
  }

  @Test
  void overdueIsCalculatedForPastDueDates() throws Exception {
    String token = registerAndGetToken();
    createTask(token, "Atrasada", "2020-01-01T00:00:00Z", "todo", 1);

    mockMvc
        .perform(get("/api/v1/tasks").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.content[0].overdue").value(true));
  }

  @Test
  void deletingTaskRemovesIt() throws Exception {
    String token = registerAndGetToken();
    String taskId = createTask(token, "Temporal", null, "todo", 1);

    mockMvc
        .perform(delete("/api/v1/tasks/" + taskId).header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/tasks/" + taskId).header("Authorization", bearer(token)))
        .andExpect(status().isNotFound());
  }

  private String createTask(String token, String title, String dueAt, String status, int priority)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/tasks")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(taskBody(title, dueAt, status, priority)))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private void createTaskInCourse(
      String token, String title, String dueAt, String status, int priority, String courseId)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tasks")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"%s","dueAt":"%s","status":"%s","priority":%d,"courseId":"%s"}
                    """
                        .formatted(title, dueAt, status, priority, courseId)))
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
                        {"name":"Álgebra","period":"2026-1"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private String taskBody(String title, String dueAt, String status, int priority) {
    String due = dueAt == null ? "null" : "\"" + dueAt + "\"";
    return """
    {"title":"%s","dueAt":%s,"status":"%s","priority":%d,
     "subtasks":[{"title":"Recoger datos","done":true},{"title":"Redactar","done":false}]}
    """
        .formatted(title, due, status, priority);
  }
}
