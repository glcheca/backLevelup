package com.levelup.levelup.task;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.task.dto.TaskRequest;
import com.levelup.levelup.task.dto.TaskResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping
  public PageResponse<TaskResponse> list(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) UUID courseId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant dueBefore,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    return taskService.list(principal.id(), status, courseId, dueBefore, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TaskResponse create(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody TaskRequest request) {
    return taskService.create(principal.id(), request);
  }

  @GetMapping("/{taskId}")
  public TaskResponse get(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID taskId) {
    return taskService.get(principal.id(), taskId);
  }

  @PatchMapping("/{taskId}")
  public TaskResponse update(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID taskId,
      @Valid @RequestBody TaskRequest request) {
    return taskService.update(principal.id(), taskId, request);
  }

  /** Marca la tarea como hecha (y fija `completed_at`). Reabrir = PATCH con `status != done`. */
  @PatchMapping("/{taskId}/complete")
  public TaskResponse complete(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID taskId) {
    return taskService.complete(principal.id(), taskId);
  }

  @DeleteMapping("/{taskId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID taskId) {
    taskService.delete(principal.id(), taskId);
  }
}
