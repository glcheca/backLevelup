package com.levelup.levelup.task;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.common.ReminderText;
import com.levelup.levelup.course.CourseService;
import com.levelup.levelup.event.EventService;
import com.levelup.levelup.reminder.ReminderHook;
import com.levelup.levelup.task.dto.SubtaskRequest;
import com.levelup.levelup.task.dto.TaskRequest;
import com.levelup.levelup.task.dto.TaskResponse;
import com.levelup.levelup.user.User;
import com.levelup.levelup.user.UserService;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

  private static final int MAX_PAGE_SIZE = 300;
  private static final int MAX_SUBTASKS = 50;

  private final TaskRepository taskRepository;
  private final EventService eventService;
  private final CourseService courseService;
  private final UserService userService;
  private final ReminderHook reminderHook;

  public TaskService(
      TaskRepository taskRepository,
      EventService eventService,
      CourseService courseService,
      UserService userService,
      ReminderHook reminderHook) {
    this.taskRepository = taskRepository;
    this.eventService = eventService;
    this.courseService = courseService;
    this.userService = userService;
    this.reminderHook = reminderHook;
  }

  @Transactional(readOnly = true)
  public PageResponse<TaskResponse> list(
      UUID userId, TaskStatus status, UUID courseId, Instant dueBefore, int page, int size) {
    Pageable pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Order.asc("dueAt").nullsLast(), Sort.Order.asc("createdAt")));
    Specification<Task> filter = filter(userId, status, courseId, dueBefore);
    return PageResponse.from(taskRepository.findAll(filter, pageable), TaskMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public TaskResponse get(UUID userId, UUID taskId) {
    return TaskMapper.toResponse(requireOwnedTask(userId, taskId));
  }

  @Transactional
  public TaskResponse create(UUID userId, TaskRequest request) {
    User user = userService.requireUser(userId);
    Task task = new Task(user, request.title().trim());
    apply(task, request, userId);
    Task saved = taskRepository.save(task);
    syncReminder(saved, user);
    return TaskMapper.toResponse(saved);
  }

  @Transactional
  public TaskResponse update(UUID userId, UUID taskId, TaskRequest request) {
    Task task = requireOwnedTask(userId, taskId);
    User user = userService.requireUser(userId);
    task.setTitle(request.title().trim());
    apply(task, request, userId);
    Task saved = taskRepository.save(task);
    syncReminder(saved, user);
    return TaskMapper.toResponse(saved);
  }

  @Transactional
  public TaskResponse complete(UUID userId, UUID taskId) {
    Task task = requireOwnedTask(userId, taskId);
    task.setStatus(TaskStatus.done);
    task.setCompletedAt(Instant.now());
    Task saved = taskRepository.save(task);
    reminderHook.onTaskRemoved(userId, saved.getId());
    return TaskMapper.toResponse(saved);
  }

  @Transactional
  public void delete(UUID userId, UUID taskId) {
    taskRepository.delete(requireOwnedTask(userId, taskId));
    reminderHook.onTaskRemoved(userId, taskId);
  }

  private void apply(Task task, TaskRequest request, UUID userId) {
    task.setDescription(trimToNull(request.description()));
    task.setDueAt(request.dueAt());
    task.setPriority(request.priority() == null ? (short) 1 : request.priority().shortValue());

    TaskStatus status = request.status() == null ? TaskStatus.todo : request.status();
    task.setStatus(status);
    if (status == TaskStatus.done) {
      if (task.getCompletedAt() == null) {
        task.setCompletedAt(Instant.now());
      }
    } else {
      task.setCompletedAt(null);
    }

    task.setCourse(
        request.courseId() == null
            ? null
            : courseService.requireOwnedCourse(userId, request.courseId()));
    task.setEvent(
        request.eventId() == null
            ? null
            : eventService.requireOwnedEvent(userId, request.eventId()));
    task.setSubtasks(validateSubtasks(request.subtasks()));
    task.setReminderOffsetMinutes(request.reminderOffsetMinutes());
  }

  /** Mantiene el recordatorio de la tarea: lo cancela si está hecha, sin fecha o desactivado. */
  private void syncReminder(Task task, User user) {
    Integer offset = task.getReminderOffsetMinutes();
    boolean enabled =
        offset != null
            && task.getDueAt() != null
            && task.getStatus() != TaskStatus.done
            && user.isNotifyTasks();
    if (!enabled) {
      reminderHook.onTaskChanged(user.getId(), task.getId(), task.getTitle(), null, null, null);
      return;
    }
    Instant triggerAt = task.getDueAt().minus(offset, ChronoUnit.MINUTES);
    String extra = task.getCourse() == null ? null : task.getCourse().getName();
    reminderHook.onTaskChanged(
        user.getId(),
        task.getId(),
        task.getTitle(),
        ReminderText.body("Vence", task.getDueAt(), ZoneId.of(user.getTimezone()), extra),
        triggerAt,
        offset);
  }

  private List<Subtask> validateSubtasks(List<SubtaskRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return new ArrayList<>();
    }
    if (requests.size() > MAX_SUBTASKS) {
      throw ApiException.badRequest("Una tarea admite como máximo " + MAX_SUBTASKS + " subtareas.");
    }

    Set<String> ids = new HashSet<>();
    List<Subtask> subtasks = new ArrayList<>(requests.size());
    for (SubtaskRequest request : requests) {
      String title = request.title() == null ? "" : request.title().trim();
      if (title.isEmpty()) {
        throw ApiException.badRequest("Cada subtarea necesita un título.");
      }
      String id =
          request.id() == null || request.id().isBlank()
              ? UUID.randomUUID().toString()
              : request.id().trim();
      if (!ids.add(id)) {
        throw ApiException.badRequest("Hay subtareas repetidas.");
      }
      subtasks.add(new Subtask(id, title, Boolean.TRUE.equals(request.done())));
    }
    return subtasks;
  }

  /** Comprueba que la tarea existe y es del usuario, sin exponer la tarea completa. */
  public Task requireOwnedTask(UUID userId, UUID taskId) {
    return taskRepository
        .findByIdAndUserId(taskId, userId)
        .orElseThrow(() -> ApiException.notFound("Tarea no encontrada."));
  }

  private Specification<Task> filter(
      UUID userId, TaskStatus status, UUID courseId, Instant dueBefore) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("user").get("id"), userId));
      if (status != null) {
        predicates.add(builder.equal(root.get("status"), status));
      }
      if (courseId != null) {
        predicates.add(builder.equal(root.get("course").get("id"), courseId));
      }
      if (dueBefore != null) {
        predicates.add(builder.lessThanOrEqualTo(root.get("dueAt"), dueBefore));
      }
      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
