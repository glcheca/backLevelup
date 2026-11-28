package com.levelup.levelup.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.levelup.levelup.config.RateLimitProperties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    properties = {
      "app.rate-limit.enabled=true",
      "app.rate-limit.max-attempts=2",
      "app.rate-limit.window-seconds=60"
    })
@ActiveProfiles("test")
class RateLimitFlowTest {

  @Autowired private WebApplicationContext context;
  @Autowired private RateLimitProperties properties;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void loginIsBlockedAfterTooManyAttempts() throws Exception {
    assertThat(properties.getMaxAttempts()).isEqualTo(2);
    String body =
        """
        {"email":"%s","password":"WrongPassword"}
        """
            .formatted("blocked-" + UUID.randomUUID() + "@levelup.test");

    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isUnauthorized());
    }

    mockMvc
        .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"));
  }
}
