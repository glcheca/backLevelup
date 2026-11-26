package com.levelup.levelup.course;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

  Page<Course> findByUserId(UUID userId, Pageable pageable);

  Page<Course> findByUserIdAndArchived(UUID userId, boolean archived, Pageable pageable);

  Optional<Course> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByUserIdAndNameAndPeriod(UUID userId, String name, String period);

  boolean existsByUserIdAndNameAndPeriodAndIdNot(UUID userId, String name, String period, UUID id);
}
