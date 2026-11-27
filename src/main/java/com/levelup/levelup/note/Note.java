package com.levelup.levelup.note;

import com.levelup.levelup.common.AuditableEntity;
import com.levelup.levelup.common.TextNormalizer;
import com.levelup.levelup.course.Course;
import com.levelup.levelup.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "note")
public class Note extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private Course course;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(name = "content_md", nullable = false, columnDefinition = "text")
  private String contentMd = "";

  @Column(nullable = false)
  private boolean pinned;

  /** Copia normalizada (sin tildes, minúsculas) de título + contenido + etiquetas. */
  @Column(name = "search_text", nullable = false, columnDefinition = "text")
  private String searchText = "";

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "note_tag", joinColumns = @JoinColumn(name = "note_id"))
  @Column(name = "tag", nullable = false, length = 40)
  private Set<String> tags = new LinkedHashSet<>();

  protected Note() {}

  public Note(User user, String title) {
    this.user = user;
    this.title = title;
    refreshSearchText();
  }

  /** Recalcula el texto indexado; se llama tras cambiar título, contenido o etiquetas. */
  public void refreshSearchText() {
    searchText = TextNormalizer.normalize(title + " " + contentMd + " " + String.join(" ", tags));
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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContentMd() {
    return contentMd;
  }

  public void setContentMd(String contentMd) {
    this.contentMd = contentMd == null ? "" : contentMd;
  }

  public boolean isPinned() {
    return pinned;
  }

  public void setPinned(boolean pinned) {
    this.pinned = pinned;
  }

  public String getSearchText() {
    return searchText;
  }

  public Set<String> getTags() {
    return tags;
  }

  public void setTags(Set<String> tags) {
    this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
  }
}
