package com.levelup.levelup.notification;

import com.levelup.levelup.common.AuditableEntity;
import com.levelup.levelup.reminder.Reminder;
import com.levelup.levelup.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "notification")
public class Notification extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  @JoinColumn(name = "reminder_id")
  private Reminder reminder;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(columnDefinition = "text")
  private String body;

  @Column(name = "read_at")
  private Instant readAt;

  protected Notification() {}

  public Notification(User user, Reminder reminder, String title, String body) {
    this.user = user;
    this.reminder = reminder;
    this.title = title;
    this.body = body;
  }

  public User getUser() {
    return user;
  }

  public Reminder getReminder() {
    return reminder;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public boolean isRead() {
    return readAt != null;
  }

  public void markRead(Instant when) {
    this.readAt = when;
  }
}
