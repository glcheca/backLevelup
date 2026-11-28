package com.levelup.levelup.task;

import com.levelup.levelup.common.AuditableEntity;
import com.levelup.levelup.course.Course;
import com.levelup.levelup.event.Event;
import com.levelup.levelup.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "task")
public class Task extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id")
  private Event event;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TaskStatus status = TaskStatus.todo;

  /** 0 = baja, 1 = media, 2 = alta. */
  @Column(nullable = false)
  private short priority = 1;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Convert(converter = SubtaskListConverter.class)
  @Column(name = "subtasks_json", nullable = false, length = 4000)
  private List<Subtask> subtasks = new ArrayList<>();

  /** Minutos de antelación del recordatorio; `null` = sin recordatorio. */
  @Column(name = "reminder_offset_minutes")
  private Integer reminderOffsetMinutes;

  protected Task() {}

  public Task(User user, String title) {
    this.user = user;
    this.title = title;
  }

  public boolean isOverdue() {
    return dueAt != null && status != TaskStatus.done && dueAt.isBefore(Instant.now());
  }

  public User getUser() {
    return user;
  }

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public short getPriority() {
    return priority;
  }

  public void setPriority(short priority) {
    this.priority = priority;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public void setDueAt(Instant dueAt) {
    this.dueAt = dueAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public List<Subtask> getSubtasks() {
    return subtasks;
  }

  public void setSubtasks(List<Subtask> subtasks) {
    this.subtasks = subtasks == null ? new ArrayList<>() : subtasks;
  }

  public Integer getReminderOffsetMinutes() {
    return reminderOffsetMinutes;
  }

  public void setReminderOffsetMinutes(Integer reminderOffsetMinutes) {
    this.reminderOffsetMinutes = reminderOffsetMinutes;
  }
}
