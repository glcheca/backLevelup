package com.levelup.levelup.grade;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.grade.dto.GradeRequest;
import com.levelup.levelup.grade.dto.GradeResponse;
import com.levelup.levelup.grade.dto.GradeSummaryResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
public class GradeController {

  private final GradeService gradeService;

  public GradeController(GradeService gradeService) {
    this.gradeService = gradeService;
  }

  @GetMapping("/courses/{courseId}/grades")
  public List<GradeResponse> list(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID courseId) {
    return gradeService.list(principal.id(), courseId);
  }

  @PostMapping("/courses/{courseId}/grades")
  @ResponseStatus(HttpStatus.CREATED)
  public GradeResponse create(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID courseId,
      @Valid @RequestBody GradeRequest request) {
    return gradeService.create(principal.id(), courseId, request);
  }

  @GetMapping("/courses/{courseId}/grades/summary")
  public GradeSummaryResponse summary(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID courseId,
      @RequestParam(required = false) BigDecimal target) {
    return gradeService.summary(principal.id(), courseId, target);
  }

  /** Resumen de todos los cursos activos (dashboard y página de notas). */
  @GetMapping("/grades/summary")
  public List<GradeSummaryResponse> summaries(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @RequestParam(required = false) BigDecimal target) {
    return gradeService.summaries(principal.id(), target);
  }

  @PatchMapping("/grades/{gradeId}")
  public GradeResponse update(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID gradeId,
      @Valid @RequestBody GradeRequest request) {
    return gradeService.update(principal.id(), gradeId, request);
  }

  @DeleteMapping("/grades/{gradeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID gradeId) {
    gradeService.delete(principal.id(), gradeId);
  }
}
