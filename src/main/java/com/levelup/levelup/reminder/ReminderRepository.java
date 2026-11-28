package com.levelup.levelup.reminder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

  Optional<Reminder> findByIdAndUserId(UUID id, UUID userId);

  Optional<Reminder> findByIdempotencyKey(String idempotencyKey);

  List<Reminder> findByUserIdOrderByTriggerAtAsc(UUID userId);

  List<Reminder> findByUserIdAndStatusOrderByTriggerAtAsc(UUID userId, ReminderStatus status);

  /**
   * Ids vencidos: el despacho reclama cada uno con un `UPDATE` condicional (seguro entre
   * instancias).
   */
  @Query(
      """
      select reminder.id from Reminder reminder
      where reminder.status = :status and reminder.triggerAt <= :now
      order by reminder.triggerAt asc
      """)
  List<UUID> findDueIds(
      @Param("status") ReminderStatus status, @Param("now") Instant now, Pageable pageable);

  /** Reclama el recordatorio: solo una instancia cambia `pending` -> `sent`. */
  @Modifying
  @Query(
      """
      update Reminder reminder
      set reminder.status = :sent, reminder.sentAt = :now
      where reminder.id = :id and reminder.status = :pending
      """)
  int claim(
      @Param("id") UUID id,
      @Param("now") Instant now,
      @Param("pending") ReminderStatus pending,
      @Param("sent") ReminderStatus sent);

  @Modifying
  @Query(
      """
      update Reminder reminder
      set reminder.status = :cancelled
      where reminder.targetType = :targetType and reminder.targetId = :targetId
        and reminder.status = :pending
      """)
  int cancelPendingForTarget(
      @Param("targetType") ReminderTargetType targetType,
      @Param("targetId") UUID targetId,
      @Param("pending") ReminderStatus pending,
      @Param("cancelled") ReminderStatus cancelled);
}
