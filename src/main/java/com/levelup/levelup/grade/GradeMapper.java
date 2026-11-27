package com.levelup.levelup.grade;

import com.levelup.levelup.course.Course;
import com.levelup.levelup.grade.dto.GradeResponse;

public final class GradeMapper {

  private GradeMapper() {}

  public static GradeResponse toResponse(Assessment assessment) {
    Course course = assessment.getCourse();
    return new GradeResponse(
        assessment.getId(),
        course.getId(),
        course.getName(),
        course.getColor(),
        assessment.getName(),
        assessment.getWeight(),
        assessment.getScore(),
        assessment.getMaxScore(),
        assessment.percentage(),
        assessment.getDueAt(),
        assessment.getNotes(),
        assessment.getCreatedAt(),
        assessment.getUpdatedAt());
  }
}
