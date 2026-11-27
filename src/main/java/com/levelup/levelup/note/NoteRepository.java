package com.levelup.levelup.note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, UUID>, JpaSpecificationExecutor<Note> {

  Optional<Note> findByIdAndUserId(UUID id, UUID userId);

  @Query(
      """
      select distinct tag from Note note join note.tags tag
      where note.user.id = :userId
      order by tag
      """)
  List<String> findTagsByUserId(@Param("userId") UUID userId);
}
