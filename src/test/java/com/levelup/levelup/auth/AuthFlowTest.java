package com.levelup.levelup.auth;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AuthFlowTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void registerReturnsAccessTokenAndRefreshCookie() throws Exception {
    String email = uniqueEmail();

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, "Password123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.user.email").value(email))
        .andExpect(result -> {
          Cookie cookie = result.getResponse().getCookie(AuthService.REFRESH_COOKIE);
          org.junit.jupiter.api.Assertions.assertNotNull(cookie);
          org.junit.jupiter.api.Assertions.assertTrue(cookie.isHttpOnly());
        });
  }

  @Test
  void duplicateRegistrationIsRejected() throws Exception {
    String email = uniqueEmail();
    String body = registerBody(email, "Password123");

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void meRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void authenticatedUserCanReadProfile() throws Exception {
    String email = uniqueEmail();
    String accessToken = register(email, "Password123");

    mockMvc
        .perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));
  }

  @Test
  void loginWithWrongPasswordFails() throws Exception {
    String email = uniqueEmail();
    register(email, "Password123");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"WrongPassword"}
                    """
                        .formatted(email)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshRotatesSessionAndLogoutRevokesIt() throws Exception {
    String email = uniqueEmail();

    MvcResult registered =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody(email, "Password123")))
            .andExpect(status().isOk())
            .andReturn();

    Cookie refreshCookie = registered.getResponse().getCookie(AuthService.REFRESH_COOKIE);

    MvcResult refreshed =
        mockMvc
            .perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

    Cookie rotatedCookie = refreshed.getResponse().getCookie(AuthService.REFRESH_COOKIE);
    org.junit.jupiter.api.Assertions.assertNotEquals(
        refreshCookie.getValue(), rotatedCookie.getValue());

    mockMvc
        .perform(post("/api/v1/auth/logout").cookie(rotatedCookie))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(rotatedCookie))
        .andExpect(status().isUnauthorized());
  }

  private String register(String email, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody(email, password)))
            .andExpect(status().isOk())
            .andReturn();

    return com.jayway.jsonpath.JsonPath.read(
        result.getResponse().getContentAsString(), "$.accessToken");
  }

  private String registerBody(String email, String password) {
    return """
    {"email":"%s","password":"%s","name":"Estudiante","timezone":"America/Lima"}
    """
        .formatted(email, password);
  }

  private String uniqueEmail() {
    return "user-" + UUID.randomUUID() + "@levelup.test";
  }
}
