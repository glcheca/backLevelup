package com.levelup.levelup.grade;

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

class GradeFlowTest extends ApiIntegrationTest {

  @Test
  void computesWeightedAverageForCompleteWeights() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Álgebra Lineal");

    createGrade(token, courseId, "Parcial 1", "30", "15", "20");
    createGrade(token, courseId, "Parcial 2", "30", "12", "20");
    createGrade(token, courseId, "Final", "40", "16", "20");

    mockMvc
        .perform(
            get("/api/v1/courses/" + courseId + "/grades/summary")
                .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weightedAverage").value(72.5))
        .andExpect(jsonPath("$.average20").value(14.5))
        .andExpect(jsonPath("$.totalWeight").value(100.0))
        .andExpect(jsonPath("$.totalWeightPending").value(0.0))
        .andExpect(jsonPath("$.weightsValid").value(true))
        .andExpect(jsonPath("$.gradedCount").value(3))
        .andExpect(jsonPath("$.message").value("Promedio final: 14.5 / 20."));
  }

  @Test
  void reportsPendingWeightAndNeededGrade() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Física II");

    createGrade(token, courseId, "Parcial 1", "30", "15", "20");
    createGrade(token, courseId, "Parcial 2", "30", "12", "20");
    createGrade(token, courseId, "Final", "40", null, "20");

    mockMvc
        .perform(
            get("/api/v1/courses/" + courseId + "/grades/summary?target=14")
                .header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.weightedAverage").value(67.5))
        .andExpect(jsonPath("$.totalWeightRegistered").value(60.0))
        .andExpect(jsonPath("$.totalWeightPending").value(40.0))
        .andExpect(jsonPath("$.weightsValid").value(true))
        .andExpect(jsonPath("$.pendingCount").value(1))
        .andExpect(jsonPath("$.neededFor").value(14.75))
        .andExpect(jsonPath("$.achievable").value(true))
        .andExpect(jsonPath("$.message").value("Necesitas 14.75 / 20 en el 40% restante."));
  }

  @Test
  void flagsWeightsThatDoNotAddUp() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Estadística");

    createGrade(token, courseId, "Parcial 1", "30", "15", "20");
    createGrade(token, courseId, "Parcial 2", "30", "12", "20");

    mockMvc
        .perform(
            get("/api/v1/courses/" + courseId + "/grades/summary")
                .header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalWeight").value(60.0))
        .andExpect(jsonPath("$.weightsValid").value(false))
        .andExpect(jsonPath("$.average20").value(13.5));
  }

  @Test
  void marksTargetAsAlreadySecuredWhenObtainedIsEnough() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Historia");

    createGrade(token, courseId, "Trabajo", "60", "20", "20");
    createGrade(token, courseId, "Examen final", "40", null, "20");

    mockMvc
        .perform(
            get("/api/v1/courses/" + courseId + "/grades/summary?target=10")
                .header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.neededFor").value(0.0))
        .andExpect(jsonPath("$.achievable").value(true))
        .andExpect(jsonPath("$.message").value("Ya tienes asegurado el 10 / 20."));
  }

  @Test
  void detectsUnreachableTargets() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Química");

    createGrade(token, courseId, "Práctica", "60", "4", "20");
    createGrade(token, courseId, "Examen final", "40", null, "20");

    mockMvc
        .perform(
            get("/api/v1/courses/" + courseId + "/grades/summary?target=18")
                .header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.neededFor").value(39.0))
        .andExpect(jsonPath("$.achievable").value(false))
        .andExpect(
            jsonPath("$.message").value("No alcanza: necesitarías 39 / 20 en el 40% restante."));
  }

  @Test
  void validatesWeightsScoresAndTargets() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Cálculo");

    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/grades")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeBody("Peso cero", "0", null, "20")))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/grades")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeBody("Nota imposible", "30", "25", "20")))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/grades")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeBody("Máximo cero", "30", null, "0")))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            get("/api/v1/courses/" + courseId + "/grades/summary?target=25")
                .header("Authorization", bearer(token)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void anotherUserCannotTouchGrades() throws Exception {
    String owner = registerAndGetToken();
    String courseId = createCourse(owner, "Álgebra Lineal");
    String gradeId = createGrade(owner, courseId, "Parcial", "50", "10", "20");

    String stranger = registerAndGetToken();

    mockMvc
        .perform(
            get("/api/v1/courses/" + courseId + "/grades")
                .header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            patch("/api/v1/grades/" + gradeId)
                .header("Authorization", bearer(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeBody("Robado", "50", "20", "20")))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(delete("/api/v1/grades/" + gradeId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/v1/courses/" + courseId + "/grades")
                .header("Authorization", bearer(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeBody("Intruso", "50", null, "20")))
        .andExpect(status().isNotFound());
  }

  @Test
  void updatesAndDeletesGrades() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Programación");

    String gradeId = createGrade(token, courseId, "Laboratorio", "40", "10", "20");

    mockMvc
        .perform(
            patch("/api/v1/grades/" + gradeId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeBody("Laboratorio", "40", "18", "20")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(18.0))
        .andExpect(jsonPath("$.percentage").value(90.0));

    mockMvc
        .perform(delete("/api/v1/grades/" + gradeId).header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/v1/courses/" + courseId + "/grades").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void summariesCoverAllActiveCourses() throws Exception {
    String token = registerAndGetToken();
    String graded = createCourse(token, "Álgebra Lineal");
    createCourse(token, "Física II");
    createGrade(token, graded, "Parcial", "100", "16", "20");

    mockMvc
        .perform(get("/api/v1/grades/summary").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath("$[?(@.courseId == '" + graded + "')].average20")
                .value(org.hamcrest.Matchers.contains(16.0)))
        .andExpect(
            jsonPath("$[?(@.courseName == 'Física II')].message")
                .value(
                    org.hamcrest.Matchers.contains(
                        "Añade tu primera evaluación para ver tu promedio.")));
  }

  private String createGrade(
      String token, String courseId, String name, String weight, String score, String maxScore)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/courses/" + courseId + "/grades")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gradeBody(name, weight, score, maxScore)))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private String gradeBody(String name, String weight, String score, String maxScore) {
    String scoreValue = score == null ? "null" : score;
    return """
    {"name":"%s","weight":%s,"score":%s,"maxScore":%s}
    """
        .formatted(name, weight, scoreValue, maxScore);
  }

  private String createCourse(String token, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/courses")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","period":"2026-2"}
                        """
                            .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }
}
