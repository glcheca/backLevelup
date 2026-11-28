package com.levelup.levelup.event;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.common.ReminderText;
import com.levelup.levelup.course.CourseService;
import com.levelup.levelup.event.dto.EventRequest;
import com.levelup.levelup.event.dto.EventResponse;
import com.levelup.levelup.reminder.ReminderHook;
import com.levelup.levelup.user.User;
import com.levelup.levelup.user.UserService;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  private static final int MAX_PAGE_SIZE = 500;

  private final EventRepository eventRepository;
  private final CourseService courseService;
  private final UserService userService;
  private final ReminderHook reminderHook;

  public EventService(
      EventRepository eventRepository,
      CourseService courseService,
      UserService userService,
      ReminderHook reminderHook) {
    this.eventRepository = eventRepository;
    this.courseService = courseService;
    this.userService = userService;
    this.reminderHook = reminderHook;
  }

  @Transactional(readOnly = true)
  public PageResponse<EventResponse> list(
      UUID userId, Instant from, Instant to, UUID courseId, EventType type, int page, int size) {
    Pageable pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by("startAt").ascending());
    Specification<Event> filter = filter(userId, from, to, courseId, type);
    return PageResponse.from(eventRepository.findAll(filter, pageable), EventMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public EventResponse get(UUID userId, UUID eventId) {
    return EventMapper.toResponse(requireOwnedEvent(userId, eventId));
  }

  @Transactional
  public EventResponse create(UUID userId, EventRequest request) {
    requireValidRange(request);
    User user = userService.requireUser(userId);
    Event event =
        new Event(user, request.title().trim(), request.type(), request.startAt(), request.endAt());
    apply(event, request, user);
    Event saved = eventRepository.save(event);
    syncReminder(saved, user);
    return EventMapper.toResponse(saved);
  }

  @Transactional
  public EventResponse update(UUID userId, UUID eventId, EventRequest request) {
    requireValidRange(request);
    Event event = requireOwnedEvent(userId, eventId);
    User user = userService.requireUser(userId);
    event.setTitle(request.title().trim());
    event.setType(request.type());
    event.setStartAt(request.startAt());
    event.setEndAt(request.endAt());
    apply(event, request, user);
    Event saved = eventRepository.save(event);
    syncReminder(saved, user);
    return EventMapper.toResponse(saved);
  }

  @Transactional
  public void delete(UUID userId, UUID eventId) {
    eventRepository.delete(requireOwnedEvent(userId, eventId));
    reminderHook.onEventRemoved(userId, eventId);
  }

  private void apply(Event event, EventRequest request, User user) {
    event.setDescription(trimToNull(request.description()));
    event.setLocation(trimToNull(request.location()));
    event.setAllDay(Boolean.TRUE.equals(request.allDay()));
    event.setStatus(request.status() == null ? EventStatus.scheduled : request.status());
    event.setReminderOffsetMinutes(request.reminderOffsetMinutes());
    event.setCourse(
        request.courseId() == null
            ? null
            : courseService.requireOwnedCourse(user.getId(), request.courseId()));
    if (event.isAllDay()) {
      normalizeAllDay(event, ZoneId.of(user.getTimezone()));
    }
  }

  /**
   * Un evento de todo el día ocupa de 00:00 a 23:59 (hora local del usuario) de su día de inicio.
   */
  private void normalizeAllDay(Event event, ZoneId zone) {
    var day = event.getStartAt().atZone(zone).toLocalDate();
    event.setStartAt(day.atStartOfDay(zone).toInstant());
    event.setEndAt(day.atTime(LocalTime.of(23, 59, 59)).atZone(zone).toInstant());
  }

  /** Mantiene el recordatorio del evento: lo cancela si no hay aviso o el usuario los desactivó. */
  private void syncReminder(Event event, User user) {
    Integer offset = event.getReminderOffsetMinutes();
    if (offset == null || !user.isNotifyEvents()) {
      reminderHook.onEventChanged(user.getId(), event.getId(), event.getTitle(), null, null, null);
      return;
    }
    Instant triggerAt = event.getStartAt().minus(offset, ChronoUnit.MINUTES);
    String extra = event.getLocation() != null ? event.getLocation() : courseName(event);
    reminderHook.onEventChanged(
        user.getId(),
        event.getId(),
        event.getTitle(),
        ReminderText.body("Empieza", event.getStartAt(), ZoneId.of(user.getTimezone()), extra),
        triggerAt,
        offset);
  }

  private static String courseName(Event event) {
    return event.getCourse() == null ? null : event.getCourse().getName();
  }

  private void requireValidRange(EventRequest request) {
    if (request.endAt().isBefore(request.startAt())) {
      throw ApiException.badRequest("La fecha de fin no puede ser anterior a la de inicio.");
    }
  }

  /** Comprueba que el evento existe y pertenece al usuario; si no, responde 404. */
  public Event requireOwnedEvent(UUID userId, UUID eventId) {
    return eventRepository
        .findByIdAndUserId(eventId, userId)
        .orElseThrow(() -> ApiException.notFound("Evento no encontrado."));
  }

  private Specification<Event> filter(
      UUID userId, Instant from, Instant to, UUID courseId, EventType type) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("user").get("id"), userId));
      if (to != null) {
        predicates.add(builder.lessThan(root.get("startAt"), to));
      }
      if (from != null) {
        predicates.add(builder.greaterThan(root.get("endAt"), from));
      }
      if (courseId != null) {
        predicates.add(builder.equal(root.get("course").get("id"), courseId));
      }
      if (type != null) {
        predicates.add(builder.equal(root.get("type"), type));
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
