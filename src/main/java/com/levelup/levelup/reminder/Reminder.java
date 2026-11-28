package com.levelup.levelup.reminder;

import com.levelup.levelup.common.AuditableEntity;
import com.levelup.levelup.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "reminder")
public class Reminder extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "target_type", nullable = false, length = 10)
  private ReminderTargetType targetType = ReminderTargetType.CUSTOM;

  @Column(name = "target_id")
  private UUID targetId;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(columnDefinition = "text")
  private String body;

  @Column(name = "trigger_at", nullable = false)
  private Instant triggerAt;

  @Column(nullable = false, length = 10)
  private String channel = "in_app";

  @Column(nullable = false, length = 10)
  private ReminderStatus status = ReminderStatus.PENDING;

  @Column(nullable = false)
  private short attempts;

  @Column(name = "sent_at")
  private Instant sentAt;

  /** `targetType:targetId:offsetMinutes`: evita duplicados si el job se reintenta. */
  @Column(name = "idempotency_key", nullable = false, length = 160, unique = true)
  private String idempotencyKey;

  protected Reminder() {}

  public Reminder(User user, ReminderTargetType targetType, UUID targetId, String idempotencyKey) {
    this.user = user;
    this.targetType = targetType;
    this.targetId = targetId;
    this.idempotencyKey = idempotencyKey;
  }

  public User getUser() {
    return user;
  }

  public ReminderTargetType getTargetType() {
    return targetType;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Instant getTriggerAt() {
    return triggerAt;
  }

  public void setTriggerAt(Instant triggerAt) {
    this.triggerAt = triggerAt;
  }

  public String getChannel() {
    return channel;
  }

  public ReminderStatus getStatus() {
    return status;
  }

  public void setStatus(ReminderStatus status) {
    this.status = status;
  }

  public short getAttempts() {
    return attempts;
  }

  public void setAttempts(short attempts) {
    this.attempts = attempts;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void setSentAt(Instant sentAt) {
    this.sentAt = sentAt;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }
}
