package com.levelup.levelup.support;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base para los tests de API: arranca el contexto con el perfil `test` (H2 en memoria), prepara
 * MockMvc y da utilidades para registrar usuarios y leer el JSON de las respuestas.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

  @Autowired protected WebApplicationContext context;

  protected MockMvc mockMvc;

  @BeforeEach
  void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  /** Registra un usuario nuevo y devuelve su access token. */
  protected String registerAndGetToken() throws Exception {
    String email = uniqueEmail();
    MvcResult result =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"Password123","name":"Estudiante","timezone":"America/Lima"}
                        """
                            .formatted(email)))
            .andReturn();

    if (result.getResponse().getStatus() != 200) {
      throw new IllegalStateException(
          "No se pudo registrar el usuario de prueba: "
              + result.getResponse().getContentAsString());
    }
    return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
  }

  protected static String uniqueEmail() {
    return "user-" + UUID.randomUUID() + "@levelup.test";
  }

  protected static String bearer(String token) {
    return "Bearer " + token;
  }
}
