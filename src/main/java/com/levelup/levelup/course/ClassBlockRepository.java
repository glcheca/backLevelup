package com.levelup.levelup.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassBlockRepository extends JpaRepository<ClassBlock, UUID> {

  List<ClassBlock> findByCourseIdOrderByDayOfWeekAscStartTimeAsc(UUID courseId);

  List<ClassBlock> findByCourseIdAndDayOfWeek(UUID courseId, short dayOfWeek);

  Optional<ClassBlock> findByIdAndCourseUserId(UUID id, UUID userId);

  List<ClassBlock> findAllByCourseUserIdOrderByDayOfWeekAscStartTimeAsc(UUID userId);
}
