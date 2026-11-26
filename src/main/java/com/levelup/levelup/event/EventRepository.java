package com.levelup.levelup.event;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository
    extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

  Optional<Event> findByIdAndUserId(UUID id, UUID userId);
}
