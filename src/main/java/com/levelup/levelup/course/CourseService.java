package com.levelup.levelup.course;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.course.dto.ClassBlockRequest;
import com.levelup.levelup.course.dto.ClassBlockResponse;
import com.levelup.levelup.course.dto.CourseRequest;
import com.levelup.levelup.course.dto.CourseResponse;
import com.levelup.levelup.course.dto.CourseUpdateRequest;
import com.levelup.levelup.user.UserRepository;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

  static final String DEFAULT_COLOR = "#6366f1";
  private static final int MAX_PAGE_SIZE = 100;
  private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

  private final CourseRepository courseRepository;
  private final ClassBlockRepository blockRepository;
  private final UserRepository userRepository;

  public CourseService(
      CourseRepository courseRepository,
      ClassBlockRepository blockRepository,
      UserRepository userRepository) {
    this.courseRepository = courseRepository;
    this.blockRepository = blockRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public PageResponse<CourseResponse> list(UUID userId, Boolean archived, int page, int size) {
    Pageable pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by("name").ascending());
    Page<Course> courses =
        archived == null
            ? courseRepository.findByUserId(userId, pageable)
            : courseRepository.findByUserIdAndArchived(userId, archived, pageable);
    return PageResponse.from(courses, CourseMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public CourseResponse get(UUID userId, UUID courseId) {
    Course course = requireOwnedCourse(userId, courseId);
    return CourseMapper.toResponse(course, scheduleOfCourse(courseId));
  }

  @Transactional
  public CourseResponse create(UUID userId, CourseRequest request) {
    String name = request.name().trim();
    String period = request.period().trim();
    if (courseRepository.existsByUserIdAndNameAndPeriod(userId, name, period)) {
      throw ApiException.conflict(
          "Ya tienes un curso con ese nombre en el periodo " + period + ".");
    }

    Course course = new Course(userRepository.getReferenceById(userId), name, period);
    course.setCode(trimToNull(request.code()));
    course.setProfessor(trimToNull(request.professor()));
    course.setColor(request.color() == null ? DEFAULT_COLOR : request.color());
    course.setCredits(request.credits());
    return CourseMapper.toResponse(courseRepository.save(course));
  }

  @Transactional
  public CourseResponse update(UUID userId, UUID courseId, CourseUpdateRequest request) {
    Course course = requireOwnedCourse(userId, courseId);

    String name = request.name() == null ? course.getName() : request.name().trim();
    String period = request.period() == null ? course.getPeriod() : request.period().trim();
    boolean identityChanged = !name.equals(course.getName()) || !period.equals(course.getPeriod());

    if (identityChanged
        && courseRepository.existsByUserIdAndNameAndPeriodAndIdNot(
            userId, name, period, courseId)) {
      throw ApiException.conflict(
          "Ya tienes un curso con ese nombre en el periodo " + period + ".");
    }

    course.setName(name);
    course.setPeriod(period);
    if (request.code() != null) {
      course.setCode(trimToNull(request.code()));
    }
    if (request.professor() != null) {
      course.setProfessor(trimToNull(request.professor()));
    }
    if (request.color() != null) {
      course.setColor(request.color());
    }
    if (request.credits() != null) {
      course.setCredits(request.credits());
    }
    return CourseMapper.toResponse(courseRepository.save(course));
  }

  @Transactional
  public CourseResponse setArchived(UUID userId, UUID courseId, boolean archived) {
    Course course = requireOwnedCourse(userId, courseId);
    course.setArchived(archived);
    return CourseMapper.toResponse(courseRepository.save(course));
  }

  @Transactional
  public void delete(UUID userId, UUID courseId) {
    courseRepository.delete(requireOwnedCourse(userId, courseId));
  }

  // --- Horario -------------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<ClassBlockResponse> listSchedule(UUID userId) {
    return blockRepository.findAllByCourseUserIdOrderByDayOfWeekAscStartTimeAsc(userId).stream()
        .map(CourseMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClassBlockResponse> scheduleOfCourse(UUID courseId) {
    return blockRepository.findByCourseIdOrderByDayOfWeekAscStartTimeAsc(courseId).stream()
        .map(CourseMapper::toResponse)
        .toList();
  }

  @Transactional
  public ClassBlockResponse createBlock(UUID userId, UUID courseId, ClassBlockRequest request) {
    Course course = requireOwnedCourse(userId, courseId);
    validateRange(request.startTime(), request.endTime());

    short day = request.dayOfWeek().shortValue();
    String warning = overlapWarning(courseId, day, request.startTime(), request.endTime(), null);
    ClassBlock block =
        new ClassBlock(
            course, day, request.startTime(), request.endTime(), trimToNull(request.room()));
    return CourseMapper.toResponse(blockRepository.save(block), warning);
  }

  @Transactional
  public ClassBlockResponse updateBlock(UUID userId, UUID blockId, ClassBlockRequest request) {
    ClassBlock block = requireOwnedBlock(userId, blockId);
    validateRange(request.startTime(), request.endTime());

    short day = request.dayOfWeek().shortValue();
    String warning =
        overlapWarning(
            block.getCourse().getId(), day, request.startTime(), request.endTime(), blockId);
    block.setDayOfWeek(day);
    block.setStartTime(request.startTime());
    block.setEndTime(request.endTime());
    block.setRoom(trimToNull(request.room()));
    return CourseMapper.toResponse(blockRepository.save(block), warning);
  }

  @Transactional
  public void deleteBlock(UUID userId, UUID blockId) {
    blockRepository.delete(requireOwnedBlock(userId, blockId));
  }

  /** Comprueba que el curso existe y pertenece al usuario; si no, responde 404. */
  public Course requireOwnedCourse(UUID userId, UUID courseId) {
    return courseRepository
        .findByIdAndUserId(courseId, userId)
        .orElseThrow(() -> ApiException.notFound("Curso no encontrado."));
  }

  private ClassBlock requireOwnedBlock(UUID userId, UUID blockId) {
    return blockRepository
        .findByIdAndCourseUserId(blockId, userId)
        .orElseThrow(() -> ApiException.notFound("Bloque de horario no encontrado."));
  }

  private void validateRange(LocalTime start, LocalTime end) {
    if (!end.isAfter(start)) {
      throw ApiException.badRequest("La hora de fin debe ser posterior a la hora de inicio.");
    }
  }

  private String overlapWarning(
      UUID courseId, short day, LocalTime start, LocalTime end, UUID excludeBlockId) {
    return blockRepository.findByCourseIdAndDayOfWeek(courseId, day).stream()
        .filter(block -> excludeBlockId == null || !block.getId().equals(excludeBlockId))
        .filter(block -> block.overlaps(day, start, end))
        .findFirst()
        .map(block ->
            "Se solapa con el bloque %s–%s del mismo curso."
                .formatted(block.getStartTime().format(HOUR), block.getEndTime().format(HOUR)))
        .orElse(null);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
