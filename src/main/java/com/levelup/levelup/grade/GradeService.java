package com.levelup.levelup.grade;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.course.Course;
import com.levelup.levelup.course.CourseRepository;
import com.levelup.levelup.course.CourseService;
import com.levelup.levelup.grade.dto.GradeRequest;
import com.levelup.levelup.grade.dto.GradeResponse;
import com.levelup.levelup.grade.dto.GradeSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeService {

  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final BigDecimal FIVE = new BigDecimal("5");
  private static final BigDecimal MAX_TARGET = new BigDecimal("20");
  private static final BigDecimal MAX_SCORE_VALUE = new BigDecimal("999.99");
  private static final BigDecimal DEFAULT_TARGET = new BigDecimal("14");
  private static final Sort ORDER =
      Sort.by(Sort.Order.asc("dueAt").nullsLast(), Sort.Order.asc("createdAt"));

  private final AssessmentRepository assessmentRepository;
  private final CourseRepository courseRepository;
  private final CourseService courseService;

  public GradeService(
      AssessmentRepository assessmentRepository,
      CourseRepository courseRepository,
      CourseService courseService) {
    this.assessmentRepository = assessmentRepository;
    this.courseRepository = courseRepository;
    this.courseService = courseService;
  }

  @Transactional(readOnly = true)
  public List<GradeResponse> list(UUID userId, UUID courseId) {
    courseService.requireOwnedCourse(userId, courseId);
    return assessmentRepository.findByCourseId(courseId, ORDER).stream()
        .map(GradeMapper::toResponse)
        .toList();
  }

  @Transactional
  public GradeResponse create(UUID userId, UUID courseId, GradeRequest request) {
    Course course = courseService.requireOwnedCourse(userId, courseId);
    Assessment assessment = new Assessment(course, request.name().trim());
    apply(assessment, request);
    return GradeMapper.toResponse(assessmentRepository.save(assessment));
  }

  @Transactional
  public GradeResponse update(UUID userId, UUID gradeId, GradeRequest request) {
    Assessment assessment = requireOwnedGrade(userId, gradeId);
    assessment.setName(request.name().trim());
    apply(assessment, request);
    return GradeMapper.toResponse(assessmentRepository.save(assessment));
  }

  @Transactional
  public void delete(UUID userId, UUID gradeId) {
    assessmentRepository.delete(requireOwnedGrade(userId, gradeId));
  }

  @Transactional(readOnly = true)
  public GradeSummaryResponse summary(UUID userId, UUID courseId, BigDecimal target) {
    Course course = courseService.requireOwnedCourse(userId, courseId);
    List<Assessment> assessments = assessmentRepository.findByCourseId(courseId, ORDER);
    return summarize(course, assessments, normalizeTarget(target));
  }

  /** Resumen de todos los cursos activos, para el dashboard. */
  @Transactional(readOnly = true)
  public List<GradeSummaryResponse> summaries(UUID userId, BigDecimal target) {
    BigDecimal effectiveTarget = normalizeTarget(target);
    List<Course> courses =
        courseRepository.findByUserIdAndArchived(userId, false, Pageable.unpaged()).getContent();

    Map<UUID, List<Assessment>> byCourse = new LinkedHashMap<>();
    for (Assessment assessment : assessmentRepository.findByCourseUserId(userId, ORDER)) {
      byCourse
          .computeIfAbsent(assessment.getCourse().getId(), key -> new ArrayList<>())
          .add(assessment);
    }

    return courses.stream()
        .map(course ->
            summarize(course, byCourse.getOrDefault(course.getId(), List.of()), effectiveTarget))
        .toList();
  }

  private GradeSummaryResponse summarize(
      Course course, List<Assessment> assessments, BigDecimal target) {
    BigDecimal obtained = BigDecimal.ZERO;
    BigDecimal registeredWeight = BigDecimal.ZERO;
    BigDecimal pendingWeight = BigDecimal.ZERO;
    int gradedCount = 0;
    int pendingCount = 0;

    for (Assessment assessment : assessments) {
      BigDecimal percentage = assessment.percentage();
      if (percentage == null) {
        pendingWeight = pendingWeight.add(assessment.getWeight());
        pendingCount++;
      } else {
        obtained = obtained.add(percentage.multiply(assessment.getWeight()));
        registeredWeight = registeredWeight.add(assessment.getWeight());
        gradedCount++;
      }
    }

    BigDecimal totalWeight = registeredWeight.add(pendingWeight);
    BigDecimal weightedAverage =
        gradedCount == 0 ? null : obtained.divide(registeredWeight, 2, RoundingMode.HALF_UP);
    BigDecimal average20 =
        weightedAverage == null ? null : weightedAverage.divide(FIVE, 2, RoundingMode.HALF_UP);
    boolean weightsValid = totalWeight.compareTo(HUNDRED) == 0;

    BigDecimal neededFor = null;
    boolean achievable;
    String message;

    if (pendingWeight.signum() > 0) {
      BigDecimal targetPercentage = target.multiply(FIVE);
      BigDecimal neededPercentage =
          targetPercentage
              .multiply(HUNDRED)
              .subtract(obtained)
              .divide(pendingWeight, 4, RoundingMode.HALF_UP);
      achievable = neededPercentage.compareTo(HUNDRED) <= 0;
      if (neededPercentage.signum() <= 0) {
        neededFor = BigDecimal.ZERO.setScale(2);
        message = "Ya tienes asegurado el %s / 20.".formatted(format(target));
      } else {
        neededFor = neededPercentage.divide(FIVE, 2, RoundingMode.HALF_UP);
        message =
            achievable
                ? "Necesitas %s / 20 en el %s%% restante."
                    .formatted(format(neededFor), format(pendingWeight))
                : "No alcanza: necesitarías %s / 20 en el %s%% restante."
                    .formatted(format(neededFor), format(pendingWeight));
      }
    } else {
      achievable = average20 != null && average20.compareTo(target) >= 0;
      message =
          average20 == null
              ? "Añade tu primera evaluación para ver tu promedio."
              : "Promedio final: %s / 20.".formatted(format(average20));
    }

    return new GradeSummaryResponse(
        course.getId(),
        course.getName(),
        course.getColor(),
        weightedAverage,
        average20,
        scale(totalWeight),
        scale(registeredWeight),
        scale(pendingWeight),
        weightsValid,
        gradedCount,
        pendingCount,
        target,
        neededFor,
        achievable,
        message);
  }

  private void apply(Assessment assessment, GradeRequest request) {
    BigDecimal weight = scale(request.weight());
    BigDecimal maxScore = scale(request.maxScore());
    BigDecimal score = request.score() == null ? null : scale(request.score());

    if (maxScore.compareTo(MAX_SCORE_VALUE) > 0) {
      throw ApiException.badRequest("La nota máxima no puede superar 999.99.");
    }
    if (score != null && score.compareTo(maxScore) > 0) {
      throw ApiException.badRequest("La nota no puede superar la nota máxima.");
    }

    assessment.setWeight(weight);
    assessment.setMaxScore(maxScore);
    assessment.setScore(score);
    assessment.setDueAt(request.dueAt());
    assessment.setNotes(trimToNull(request.notes()));
  }

  private Assessment requireOwnedGrade(UUID userId, UUID gradeId) {
    return assessmentRepository
        .findByIdAndCourseUserId(gradeId, userId)
        .orElseThrow(() -> ApiException.notFound("Evaluación no encontrada."));
  }

  private static BigDecimal normalizeTarget(BigDecimal target) {
    BigDecimal value = target == null ? DEFAULT_TARGET : target;
    if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MAX_TARGET) > 0) {
      throw ApiException.badRequest("La meta debe estar entre 0 y 20.");
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal scale(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private static String format(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
