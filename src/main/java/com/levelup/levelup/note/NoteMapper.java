package com.levelup.levelup.note;

import com.levelup.levelup.course.Course;
import com.levelup.levelup.note.dto.NoteResponse;
import com.levelup.levelup.note.dto.NoteSummaryResponse;
import java.util.List;

public final class NoteMapper {

  private static final int EXCERPT_LENGTH = 180;

  private NoteMapper() {}

  public static NoteResponse toResponse(Note note) {
    Course course = note.getCourse();
    return new NoteResponse(
        note.getId(),
        note.getTitle(),
        note.getContentMd(),
        course == null ? null : course.getId(),
        course == null ? null : course.getName(),
        course == null ? null : course.getColor(),
        sortedTags(note),
        note.isPinned(),
        note.getCreatedAt(),
        note.getUpdatedAt());
  }

  public static NoteSummaryResponse toSummary(Note note) {
    Course course = note.getCourse();
    return new NoteSummaryResponse(
        note.getId(),
        note.getTitle(),
        course == null ? null : course.getId(),
        course == null ? null : course.getName(),
        course == null ? null : course.getColor(),
        sortedTags(note),
        note.isPinned(),
        excerpt(note.getContentMd()),
        note.getCreatedAt(),
        note.getUpdatedAt());
  }

  private static List<String> sortedTags(Note note) {
    return note.getTags().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
  }

  /** Texto plano corto para la lista; el markdown completo solo se envía en el detalle. */
  static String excerpt(String contentMd) {
    if (contentMd == null || contentMd.isBlank()) {
      return "";
    }
    String plain = contentMd.replaceAll("[#*_>`~\\[\\]()!]", " ").replaceAll("\\s+", " ").trim();
    if (plain.length() <= EXCERPT_LENGTH) {
      return plain;
    }
    return plain.substring(0, EXCERPT_LENGTH).trim() + "…";
  }
}
