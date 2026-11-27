package com.levelup.levelup.note;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.levelup.levelup.support.ApiIntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class NoteFlowTest extends ApiIntegrationTest {

  @Test
  void createsNoteAndReadsItBackWithContent() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Álgebra Lineal");

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/notes")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        noteBody(
                            "Exámenes finales",
                            "## Repaso\n\nDeterminantes y **matrices** inversas.",
                            courseId,
                            "[\"examen\",\"unidad-1\"]",
                            true)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Exámenes finales"))
            .andExpect(jsonPath("$.courseName").value("Álgebra Lineal"))
            .andExpect(jsonPath("$.tags.length()").value(2))
            .andExpect(jsonPath("$.tags[0]").value("examen"))
            .andExpect(jsonPath("$.pinned").value(true))
            .andReturn();

    String noteId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    MvcResult reloaded =
        mockMvc
            .perform(get("/api/v1/notes/" + noteId).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn();

    String body = reloaded.getResponse().getContentAsString(StandardCharsets.UTF_8);
    org.junit.jupiter.api.Assertions.assertTrue(body.contains("**matrices** inversas."));

    mockMvc
        .perform(get("/api/v1/notes?courseId=" + courseId).header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].excerpt").isNotEmpty())
        .andExpect(jsonPath("$.content[0].contentMd").doesNotExist());
  }

  @Test
  void searchesWithoutAccentsAndRequiresAllTerms() throws Exception {
    String token = registerAndGetToken();
    createNote(token, "Exámenes finales", "Repaso de álgebra lineal y determinantes.", null, "[]");
    createNote(token, "Física", "Óptica, lentes y ondas electromagnéticas.", null, "[]");

    expectSearchCount(token, "examen", 1);
    expectSearchCount(token, "EXAMEN", 1);
    expectSearchCount(token, "algebra", 1);
    expectSearchCount(token, "optica", 1);
    expectSearchCount(token, "examen finales", 1);
    expectSearchCount(token, "examen optica", 0);
  }

  @Test
  void filtersByTagCourseAndPinned() throws Exception {
    String token = registerAndGetToken();
    String algebra = createCourse(token, "Álgebra Lineal");
    String fisica = createCourse(token, "Física II");

    createNote(token, "Matrices", "Contenido", algebra, "[\"examen\",\"unidad-1\"]");
    createNote(token, "Óptica", "Contenido", fisica, "[\"laboratorio\"]");
    createNote(token, "Suelto", "Contenido", null, "[]");

    mockMvc
        .perform(get("/api/v1/notes?tag=examen").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(get("/api/v1/notes?tag=EXAMEN").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(get("/api/v1/notes?courseId=" + fisica).header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Óptica"));

    mockMvc
        .perform(get("/api/v1/notes?pinned=true").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(0));

    mockMvc
        .perform(
            get("/api/v1/notes?tag=examen&courseId=" + fisica)
                .header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void pinnedNotesComeFirst() throws Exception {
    String token = registerAndGetToken();
    createNote(token, "Normal", "Contenido", null, "[]");
    createNote(token, "Fijado", "Contenido", null, "[]");
    String pinnedId = findNoteId(token, "Fijado");
    pin(token, pinnedId, true);

    mockMvc
        .perform(get("/api/v1/notes").header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.content[0].title").value("Fijado"))
        .andExpect(jsonPath("$.content[0].pinned").value(true));
  }

  @Test
  void listsDistinctTags() throws Exception {
    String token = registerAndGetToken();
    createNote(token, "Uno", "Contenido", null, "[\"examen\",\"unidad-1\"]");
    createNote(token, "Dos", "Contenido", null, "[\"examen\",\"laboratorio\"]");

    mockMvc
        .perform(get("/api/v1/notes/tags").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0]").value("examen"))
        .andExpect(jsonPath("$[1]").value("laboratorio"))
        .andExpect(jsonPath("$[2]").value("unidad-1"));
  }

  @Test
  void anotherUserCannotReadOrTouchNotes() throws Exception {
    String owner = registerAndGetToken();
    String noteId = createNote(owner, "Privado", "Contenido", null, "[\"examen\"]");

    String stranger = registerAndGetToken();

    mockMvc
        .perform(get("/api/v1/notes").header("Authorization", bearer(stranger)))
        .andExpect(jsonPath("$.totalElements").value(0));

    mockMvc
        .perform(get("/api/v1/notes/" + noteId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            patch("/api/v1/notes/" + noteId)
                .header("Authorization", bearer(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(noteBody("Robado", "Contenido", null, "[]", false)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(delete("/api/v1/notes/" + noteId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/v1/notes/tags").header("Authorization", bearer(stranger)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void rejectsInvalidNotes() throws Exception {
    String token = registerAndGetToken();

    mockMvc
        .perform(
            post("/api/v1/notes")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(noteBody("   ", "Contenido", null, "[]", false)))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/notes")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    noteBody(
                        "Muchas etiquetas",
                        "Contenido",
                        null,
                        "[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\",\"k\"]",
                        false)))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/notes")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    noteBody(
                        "Etiqueta larga",
                        "Contenido",
                        null,
                        "[\"" + "x".repeat(41) + "\"]",
                        false)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updatingUnlinksCourseButKeepsContent() throws Exception {
    String token = registerAndGetToken();
    String courseId = createCourse(token, "Física II");
    String noteId = createNote(token, "Laboratorio", "Contenido inicial", courseId, "[]");

    mockMvc
        .perform(
            patch("/api/v1/notes/" + noteId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(noteBody("Laboratorio", "Contenido actualizado", null, "[]", false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courseId").doesNotExist())
        .andExpect(jsonPath("$.contentMd").value("Contenido actualizado"));

    mockMvc
        .perform(get("/api/v1/notes?courseId=" + courseId).header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  private void expectSearchCount(String token, String q, int expected) throws Exception {
    mockMvc
        .perform(get("/api/v1/notes").param("q", q).header("Authorization", bearer(token)))
        .andExpect(jsonPath("$.totalElements").value(expected));
  }

  private void pin(String token, String noteId, boolean pinned) throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/notes/" + noteId + "/pin")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pinned\":%s}".formatted(pinned)))
        .andExpect(status().isOk());
  }

  private String findNoteId(String token, String title) throws Exception {
    MvcResult result =
        mockMvc.perform(get("/api/v1/notes").header("Authorization", bearer(token))).andReturn();
    java.util.List<String> ids =
        JsonPath.read(result.getResponse().getContentAsString(), "$.content[*].id");
    java.util.List<String> titles =
        JsonPath.read(result.getResponse().getContentAsString(), "$.content[*].title");
    return ids.get(titles.indexOf(title));
  }

  private String createNote(
      String token, String title, String content, String courseId, String tags) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/notes")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(noteBody(title, content, courseId, tags, false)))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private String noteBody(
      String title, String content, String courseId, String tags, boolean pinned) {
    String course = courseId == null ? "null" : "\"" + courseId + "\"";
    return """
    {"title":"%s","contentMd":"%s","courseId":%s,"tags":%s,"pinned":%s}
    """
        .formatted(
            title,
            content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"),
            course,
            tags,
            pinned);
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
