package com.levelup.levelup.course;

import com.levelup.levelup.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "class_block")
public class ClassBlock extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  /** 1 = lunes … 7 = domingo (ISO-8601). */
  @Column(name = "day_of_week", nullable = false)
  private short dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(length = 60)
  private String room;

  protected ClassBlock() {}

  public ClassBlock(
      Course course, short dayOfWeek, LocalTime startTime, LocalTime endTime, String room) {
    this.course = course;
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime;
    this.endTime = endTime;
    this.room = room;
  }

  public boolean overlaps(short day, LocalTime from, LocalTime to) {
    return dayOfWeek == day && startTime.isBefore(to) && endTime.isAfter(from);
  }

  public Course getCourse() {
    return course;
  }

  public short getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(short dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room;
  }
}
