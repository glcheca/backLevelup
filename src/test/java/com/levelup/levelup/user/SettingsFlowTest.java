package com.levelup.levelup.user;

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

class SettingsFlowTest extends ApiIntegrationTest {

  @Test
  void updatesNotificationPreferences() throws Exception {
    Session session = register();

    mockMvc
        .perform(
            patch("/api/v1/me")
                .header("Authorization", bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notifyEvents\":false,\"notifyTasks\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notifyEvents").value(false))
        .andExpect(jsonPath("$.notifyTasks").value(false));

    mockMvc
        .perform(get("/api/v1/me").header("Authorization", bearer(session.token())))
        .andExpect(jsonPath("$.notifyEvents").value(false));
  }

  @Test
  void changesThePasswordOnlyWithTheCurrentOne() throws Exception {
    Session session = register();

    mockMvc
        .perform(
            post("/api/v1/me/password")
                .header("Authorization", bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"currentPassword":"Incorrecta123","newPassword":"NuevaClave123"}
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/me/password")
                .header("Authorization", bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"currentPassword":"Password123","newPassword":"NuevaClave123"}
                    """))
        .andExpect(status().isNoContent());

    login(session.email(), "Password123", 401);
    login(session.email(), "NuevaClave123", 200);
  }

  @Test
  void deletesTheAccountAndItsData() throws Exception {
    Session session = register();
    mockMvc
        .perform(
            post("/api/v1/courses")
                .header("Authorization", bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Temporal\",\"period\":\"2026-2\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(delete("/api/v1/me").header("Authorization", bearer(session.token())))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/me").header("Authorization", bearer(session.token())))
        .andExpect(status().isNotFound());

    login(session.email(), "Password123", 401);
  }

  private void login(String email, String password, int expectedStatus) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"%s"}
                    """
                        .formatted(email, password)))
        .andExpect(status().is(expectedStatus));
  }

  private Session register() throws Exception {
    String email = uniqueEmail();
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"Password123","name":"Ajustes"}
                        """
                            .formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
    return new Session(
        email, JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken"));
  }

  private record Session(String email, String token) {}
}
