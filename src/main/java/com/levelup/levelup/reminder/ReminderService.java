package com.levelup.levelup.reminder;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.event.EventService;
import com.levelup.levelup.reminder.dto.ReminderRequest;
import com.levelup.levelup.reminder.dto.ReminderResponse;
import com.levelup.levelup.task.TaskService;
import com.levelup.levelup.user.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {

  private final ReminderRepository reminderRepository;
  private final EventService eventService;
  private final TaskService taskService;
  private final UserService userService;

  public ReminderService(
      ReminderRepository reminderRepository,
      EventService eventService,
      TaskService taskService,
      UserService userService) {
    this.reminderRepository = reminderRepository;
    this.eventService = eventService;
    this.taskService = taskService;
    this.userService = userService;
  }

  @Transactional(readOnly = true)
  public List<ReminderResponse> list(UUID userId, ReminderStatus status) {
    List<Reminder> reminders =
        status == null
            ? reminderRepository.findByUserIdOrderByTriggerAtAsc(userId)
            : reminderRepository.findByUserIdAndStatusOrderByTriggerAtAsc(userId, status);
    return reminders.stream().map(reminder -> ReminderMapper.toResponse(reminder, null)).toList();
  }

  @Transactional
  public ReminderResponse create(UUID userId, ReminderRequest request) {
    ReminderTargetType targetType =
        request.targetType() == null ? ReminderTargetType.CUSTOM : request.targetType();
    UUID targetId = validateTarget(userId, targetType, request.targetId());

    Reminder reminder =
        new Reminder(
            userService.requireUser(userId),
            targetType,
            targetId,
            "%s:%s:%s"
                .formatted(
                    targetType.value(), targetId == null ? "none" : targetId, UUID.randomUUID()));
    reminder.setTitle(request.title().trim());
    reminder.setBody(trimToNull(request.body()));
    reminder.setTriggerAt(request.triggerAt());
    return ReminderMapper.toResponse(reminderRepository.save(reminder), null);
  }

  @Transactional
  public void delete(UUID userId, UUID reminderId) {
    Reminder reminder =
        reminderRepository
            .findByIdAndUserId(reminderId, userId)
            .orElseThrow(() -> ApiException.notFound("Recordatorio no encontrado."));
    reminderRepository.delete(reminder);
  }

  private UUID validateTarget(UUID userId, ReminderTargetType targetType, UUID targetId) {
    switch (targetType) {
      case CUSTOM -> {
        if (targetId != null) {
          throw ApiException.badRequest("Un recordatorio propio no puede apuntar a otra entidad.");
        }
        return null;
      }
      case EVENT -> {
        if (targetId == null) {
          throw ApiException.badRequest("Indica el evento del recordatorio.");
        }
        eventService.requireOwnedEvent(userId, targetId);
        return targetId;
      }
      case TASK -> {
        if (targetId == null) {
          throw ApiException.badRequest("Indica la tarea del recordatorio.");
        }
        taskService.requireOwnedTask(userId, targetId);
        return targetId;
      }
      default -> throw ApiException.badRequest("Tipo de recordatorio inválido.");
    }
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
