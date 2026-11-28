package com.levelup.levelup.event;

import com.levelup.levelup.course.Course;
import com.levelup.levelup.event.dto.EventResponse;

public final class EventMapper {

  private EventMapper() {}

  public static EventResponse toResponse(Event event) {
    Course course = event.getCourse();
    return new EventResponse(
        event.getId(),
        event.getTitle(),
        event.getDescription(),
        course == null ? null : course.getId(),
        course == null ? null : course.getName(),
        course == null ? null : course.getColor(),
        event.getType(),
        event.getStartAt(),
        event.getEndAt(),
        event.isAllDay(),
        event.getLocation(),
        event.getStatus(),
        event.getReminderOffsetMinutes(),
        event.getCreatedAt(),
        event.getUpdatedAt());
  }
}
