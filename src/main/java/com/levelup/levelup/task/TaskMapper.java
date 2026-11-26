package com.levelup.levelup.task;

import com.levelup.levelup.course.Course;
import com.levelup.levelup.task.dto.TaskResponse;
import java.util.List;

public final class TaskMapper {

  private TaskMapper() {}

  public static TaskResponse toResponse(Task task) {
    Course course = task.getCourse();
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        course == null ? null : course.getId(),
        course == null ? null : course.getName(),
        course == null ? null : course.getColor(),
        task.getEvent() == null ? null : task.getEvent().getId(),
        task.getStatus(),
        task.getPriority(),
        task.getDueAt(),
        task.getCompletedAt(),
        task.isOverdue(),
        List.copyOf(task.getSubtasks()),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }
}
