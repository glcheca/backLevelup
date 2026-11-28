package com.levelup.levelup.auth;

import com.levelup.levelup.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "user_agent", length = 255)
  private String userAgent;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected RefreshToken() {}

  public RefreshToken(User user, String tokenHash, Instant expiresAt, String userAgent) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.userAgent = userAgent;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = Instant.now();
  }

  public boolean isActive() {
    return revokedAt == null && expiresAt.isAfter(Instant.now());
  }

  public void revoke() {
    this.revokedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public String getUserAgent() {
    return userAgent;
  }
}
