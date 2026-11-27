package com.levelup.levelup.note;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.common.TextNormalizer;
import com.levelup.levelup.course.CourseService;
import com.levelup.levelup.note.dto.NoteRequest;
import com.levelup.levelup.note.dto.NoteResponse;
import com.levelup.levelup.note.dto.NoteSummaryResponse;
import com.levelup.levelup.user.UserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

  private static final int MAX_PAGE_SIZE = 100;
  private static final int MAX_TAGS = 10;
  private static final Pattern LIKE_SPECIALS = Pattern.compile("[\\\\%_]");

  private final NoteRepository noteRepository;
  private final CourseService courseService;
  private final UserService userService;

  public NoteService(
      NoteRepository noteRepository, CourseService courseService, UserService userService) {
    this.noteRepository = noteRepository;
    this.courseService = courseService;
    this.userService = userService;
  }

  @Transactional(readOnly = true)
  public PageResponse<NoteSummaryResponse> list(
      UUID userId, UUID courseId, String tag, String q, Boolean pinned, int page, int size) {
    if (courseId != null) {
      courseService.requireOwnedCourse(userId, courseId);
    }
    Pageable pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("updatedAt")));
    return PageResponse.from(
        noteRepository.findAll(filter(userId, courseId, tag, q, pinned), pageable),
        NoteMapper::toSummary);
  }

  @Transactional(readOnly = true)
  public NoteResponse get(UUID userId, UUID noteId) {
    return NoteMapper.toResponse(requireOwnedNote(userId, noteId));
  }

  @Transactional(readOnly = true)
  public List<String> tags(UUID userId) {
    return noteRepository.findTagsByUserId(userId);
  }

  @Transactional
  public NoteResponse create(UUID userId, NoteRequest request) {
    Note note = new Note(userService.requireUser(userId), request.title().trim());
    apply(note, request, userId);
    return NoteMapper.toResponse(noteRepository.save(note));
  }

  @Transactional
  public NoteResponse update(UUID userId, UUID noteId, NoteRequest request) {
    Note note = requireOwnedNote(userId, noteId);
    note.setTitle(request.title().trim());
    apply(note, request, userId);
    return NoteMapper.toResponse(noteRepository.save(note));
  }

  @Transactional
  public NoteResponse setPinned(UUID userId, UUID noteId, boolean pinned) {
    Note note = requireOwnedNote(userId, noteId);
    note.setPinned(pinned);
    return NoteMapper.toResponse(noteRepository.save(note));
  }

  @Transactional
  public void delete(UUID userId, UUID noteId) {
    noteRepository.delete(requireOwnedNote(userId, noteId));
  }

  private void apply(Note note, NoteRequest request, UUID userId) {
    note.setContentMd(request.contentMd() == null ? "" : request.contentMd());
    note.setPinned(Boolean.TRUE.equals(request.pinned()));
    note.setCourse(
        request.courseId() == null
            ? null
            : courseService.requireOwnedCourse(userId, request.courseId()));
    note.setTags(normalizeTags(request.tags()));
    note.refreshSearchText();
  }

  private Set<String> normalizeTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return new LinkedHashSet<>();
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (String tag : tags) {
      String value = tag == null ? "" : tag.trim();
      if (!value.isEmpty()) {
        normalized.add(value);
      }
    }
    if (normalized.size() > MAX_TAGS) {
      throw ApiException.badRequest(
          "Un apunte admite como máximo " + MAX_TAGS + " etiquetas distintas.");
    }
    return normalized;
  }

  private Note requireOwnedNote(UUID userId, UUID noteId) {
    return noteRepository
        .findByIdAndUserId(noteId, userId)
        .orElseThrow(() -> ApiException.notFound("Apunte no encontrado."));
  }

  private Specification<Note> filter(
      UUID userId, UUID courseId, String tag, String q, Boolean pinned) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("user").get("id"), userId));

      if (courseId != null) {
        predicates.add(builder.equal(root.get("course").get("id"), courseId));
      }
      if (pinned != null) {
        predicates.add(builder.equal(root.get("pinned"), pinned));
      }
      if (tag != null && !tag.isBlank()) {
        Join<Note, String> tags = root.join("tags");
        predicates.add(builder.equal(builder.lower(tags), tag.trim().toLowerCase(Locale.ROOT)));
      }
      for (String term : searchTerms(q)) {
        predicates.add(
            builder.like(
                builder.lower(root.get("searchText").as(String.class)), "%" + term + "%", '\\'));
      }
      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static List<String> searchTerms(String q) {
    if (q == null || q.isBlank()) {
      return List.of();
    }
    List<String> terms = new ArrayList<>();
    for (String term : TextNormalizer.normalize(q).split(" ")) {
      if (!term.isEmpty()) {
        terms.add(LIKE_SPECIALS.matcher(term).replaceAll("\\\\$0"));
      }
    }
    return terms;
  }
}
