package com.levelup.levelup.reminder;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.reminder.dto.ReminderRequest;
import com.levelup.levelup.reminder.dto.ReminderResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

  private final ReminderService reminderService;

  public ReminderController(ReminderService reminderService) {
    this.reminderService = reminderService;
  }

  @GetMapping
  public List<ReminderResponse> list(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @RequestParam(required = false) ReminderStatus status) {
    return reminderService.list(principal.id(), status);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReminderResponse create(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody ReminderRequest request) {
    return reminderService.create(principal.id(), request);
  }

  @DeleteMapping("/{reminderId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID reminderId) {
    reminderService.delete(principal.id(), reminderId);
  }
}
