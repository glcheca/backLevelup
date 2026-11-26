package com.levelup.levelup.course;

import com.levelup.levelup.course.dto.ClassBlockResponse;
import com.levelup.levelup.course.dto.CourseResponse;
import java.util.List;

public final class CourseMapper {

  private CourseMapper() {}

  public static CourseResponse toResponse(Course course) {
    return toResponse(course, CourseResponse.NO_SCHEDULE);
  }

  public static CourseResponse toResponse(Course course, List<ClassBlockResponse> schedule) {
    return new CourseResponse(
        course.getId(),
        course.getName(),
        course.getCode(),
        course.getProfessor(),
        course.getColor(),
        course.getCredits(),
        course.getPeriod(),
        course.isArchived(),
        course.getCreatedAt(),
        course.getUpdatedAt(),
        schedule);
  }

  public static ClassBlockResponse toResponse(ClassBlock block) {
    return toResponse(block, null);
  }

  public static ClassBlockResponse toResponse(ClassBlock block, String warning) {
    Course course = block.getCourse();
    return new ClassBlockResponse(
        block.getId(),
        course.getId(),
        course.getName(),
        course.getColor(),
        block.getDayOfWeek(),
        block.getStartTime(),
        block.getEndTime(),
        block.getRoom(),
        warning);
  }
}
