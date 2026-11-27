package com.levelup.levelup.grade;

import com.levelup.levelup.common.AuditableEntity;
import com.levelup.levelup.course.Course;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "assessment")
public class Assessment extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(nullable = false, length = 120)
  private String name;

  /** Porcentaje que esta evaluación aporta a la nota final del curso. */
  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal weight;

  @Column(precision = 5, scale = 2)
  private BigDecimal score;

  @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
  private BigDecimal maxScore;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(columnDefinition = "text")
  private String notes;

  protected Assessment() {}

  public Assessment(Course course, String name) {
    this.course = course;
    this.name = name;
  }

  /** Nota normalizada a 0–100; `null` si aún no está calificada. */
  public BigDecimal percentage() {
    if (score == null) {
      return null;
    }
    return score.multiply(BigDecimal.valueOf(100)).divide(maxScore, 4, RoundingMode.HALF_UP);
  }

  public Course getCourse() {
    return course;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getWeight() {
    return weight;
  }

  public void setWeight(BigDecimal weight) {
    this.weight = weight;
  }

  public BigDecimal getScore() {
    return score;
  }

  public void setScore(BigDecimal score) {
    this.score = score;
  }

  public BigDecimal getMaxScore() {
    return maxScore;
  }

  public void setMaxScore(BigDecimal maxScore) {
    this.maxScore = maxScore;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public void setDueAt(Instant dueAt) {
    this.dueAt = dueAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
