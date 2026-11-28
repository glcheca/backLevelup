package com.levelup.levelup.search;

import com.levelup.levelup.common.TextNormalizer;
import com.levelup.levelup.course.CourseService;
import com.levelup.levelup.event.EventService;
import com.levelup.levelup.note.NoteService;
import com.levelup.levelup.task.TaskService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Búsqueda global sencilla: reutiliza los listados de cada módulo y filtra en memoria (el volumen
 * del MVP es pequeño). Los apuntes usan su búsqueda normalizada, así que también ignora tildes.
 */
@Service
public class SearchService {

  private static final int LIMIT = 5;

  private final CourseService courseService;
  private final EventService eventService;
  private final TaskService taskService;
  private final NoteService noteService;

  public SearchService(
      CourseService courseService,
      EventService eventService,
      TaskService taskService,
      NoteService noteService) {
    this.courseService = courseService;
    this.eventService = eventService;
    this.taskService = taskService;
    this.noteService = noteService;
  }

  @Transactional(readOnly = true)
  public SearchResponse search(UUID userId, String query) {
    String needle = TextNormalizer.normalize(query);
    if (needle.isEmpty()) {
      return SearchResponse.empty();
    }

    List<SearchItem> courses =
        courseService.list(userId, false, 0, 100).content().stream()
            .filter(course -> matches(needle, course.name(), course.code(), course.professor()))
            .limit(LIMIT)
            .map(course ->
                new SearchItem(
                    course.id(), course.name(), join(course.code(), course.period()), "course"))
            .toList();

    List<SearchItem> events =
        eventService
            .list(userId, Instant.now().minus(365, ChronoUnit.DAYS), null, null, null, 0, 300)
            .content()
            .stream()
            .filter(event -> matches(needle, event.title(), event.description(), event.location()))
            .limit(LIMIT)
            .map(event ->
                new SearchItem(
                    event.id(), event.title(), join(event.courseName(), event.location()), "event"))
            .toList();

    List<SearchItem> tasks =
        taskService.list(userId, null, null, null, 0, 300).content().stream()
            .filter(task -> matches(needle, task.title(), task.description()))
            .limit(LIMIT)
            .map(task -> new SearchItem(task.id(), task.title(), task.courseName(), "task"))
            .toList();

    List<SearchItem> notes =
        noteService.list(userId, null, null, query, null, 0, LIMIT).content().stream()
            .map(note -> new SearchItem(note.id(), note.title(), note.excerpt(), "note"))
            .toList();

    return new SearchResponse(courses, events, tasks, notes);
  }

  private static boolean matches(String needle, String... values) {
    return Arrays.stream(values)
        .filter(value -> value != null && !value.isBlank())
        .anyMatch(value -> TextNormalizer.normalize(value).contains(needle));
  }

  private static String join(String first, String second) {
    if (first == null || first.isBlank()) {
      return second;
    }
    if (second == null || second.isBlank()) {
      return first;
    }
    return first + " · " + second;
  }
}
