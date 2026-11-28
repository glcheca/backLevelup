package com.levelup.levelup.course;

import com.levelup.levelup.common.AuditableEntity;
import com.levelup.levelup.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "course")
public class Course extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(length = 20)
  private String code;

  @Column(length = 120)
  private String professor;

  @Column(nullable = false, length = 7)
  private String color = "#6366f1";

  @Column(precision = 4, scale = 1)
  private BigDecimal credits;

  @Column(nullable = false, length = 7)
  private String period;

  @Column(nullable = false)
  private boolean archived;

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ClassBlock> blocks = new ArrayList<>();

  protected Course() {}

  public Course(User user, String name, String period) {
    this.user = user;
    this.name = name;
    this.period = period;
  }

  public User getUser() {
    return user;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getProfessor() {
    return professor;
  }

  public void setProfessor(String professor) {
    this.professor = professor;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public BigDecimal getCredits() {
    return credits;
  }

  public void setCredits(BigDecimal credits) {
    this.credits = credits;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public boolean isArchived() {
    return archived;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  public List<ClassBlock> getBlocks() {
    return blocks;
  }
}
