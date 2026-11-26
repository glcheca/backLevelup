package com.levelup.levelup.course;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.course.dto.ArchiveRequest;
import com.levelup.levelup.course.dto.ClassBlockRequest;
import com.levelup.levelup.course.dto.ClassBlockResponse;
import com.levelup.levelup.course.dto.CourseRequest;
import com.levelup.levelup.course.dto.CourseResponse;
import com.levelup.levelup.course.dto.CourseUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1")
public class CourseController {

  private final CourseService courseService;

  public CourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  @GetMapping("/courses")
  public PageResponse<CourseResponse> list(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @RequestParam(required = false) Boolean archived,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return courseService.list(principal.id(), archived, page, size);
  }

  @PostMapping("/courses")
  @ResponseStatus(HttpStatus.CREATED)
  public CourseResponse create(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody CourseRequest request) {
    return courseService.create(principal.id(), request);
  }

  @GetMapping("/courses/{courseId}")
  public CourseResponse get(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID courseId) {
    return courseService.get(principal.id(), courseId);
  }

  @PatchMapping("/courses/{courseId}")
  public CourseResponse update(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID courseId,
      @Valid @RequestBody CourseUpdateRequest request) {
    return courseService.update(principal.id(), courseId, request);
  }

  @PatchMapping("/courses/{courseId}/archive")
  public CourseResponse archive(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID courseId,
      @Valid @RequestBody ArchiveRequest request) {
    return courseService.setArchived(principal.id(), courseId, request.archived());
  }

  @DeleteMapping("/courses/{courseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID courseId) {
    courseService.delete(principal.id(), courseId);
  }

  @GetMapping("/courses/{courseId}/schedule")
  public List<ClassBlockResponse> schedule(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID courseId) {
    return courseService.get(principal.id(), courseId).schedule();
  }

  @PostMapping("/courses/{courseId}/schedule")
  @ResponseStatus(HttpStatus.CREATED)
  public ClassBlockResponse createBlock(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID courseId,
      @Valid @RequestBody ClassBlockRequest request) {
    return courseService.createBlock(principal.id(), courseId, request);
  }

  /** Horario completo del usuario: lo usa el calendario para pintar las clases recurrentes. */
  @GetMapping("/schedule")
  public List<ClassBlockResponse> allSchedule(
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return courseService.listSchedule(principal.id());
  }

  @PatchMapping("/schedule/{blockId}")
  public ClassBlockResponse updateBlock(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID blockId,
      @Valid @RequestBody ClassBlockRequest request) {
    return courseService.updateBlock(principal.id(), blockId, request);
  }

  @DeleteMapping("/schedule/{blockId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteBlock(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID blockId) {
    courseService.deleteBlock(principal.id(), blockId);
  }
}
