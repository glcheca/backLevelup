package com.levelup.levelup.event;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.event.dto.EventRequest;
import com.levelup.levelup.event.dto.EventResponse;
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
@RequestMapping("/api/v1/events")
public class EventController {

  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  /** Rango por solapamiento: eventos que empiezan antes de `to` y terminan después de `from`. */
  @GetMapping
  public PageResponse<EventResponse> list(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false) UUID courseId,
      @RequestParam(required = false) EventType type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    return eventService.list(principal.id(), from, to, courseId, type, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EventResponse create(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody EventRequest request) {
    return eventService.create(principal.id(), request);
  }

  @GetMapping("/{eventId}")
  public EventResponse get(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID eventId) {
    return eventService.get(principal.id(), eventId);
  }

  @PatchMapping("/{eventId}")
  public EventResponse update(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID eventId,
      @Valid @RequestBody EventRequest request) {
    return eventService.update(principal.id(), eventId, request);
  }

  @DeleteMapping("/{eventId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID eventId) {
    eventService.delete(principal.id(), eventId);
  }
}
