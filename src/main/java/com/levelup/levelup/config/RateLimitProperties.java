package com.levelup.levelup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Límites de peticiones por IP para los endpoints sensibles (login, registro y contraseña). */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

  private boolean enabled = true;
  private int windowSeconds = 60;
  private int maxAttempts = 10;
  private int maxPasswordAttempts = 5;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getWindowSeconds() {
    return windowSeconds;
  }

  public void setWindowSeconds(int windowSeconds) {
    this.windowSeconds = windowSeconds;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public int getMaxPasswordAttempts() {
    return maxPasswordAttempts;
  }

  public void setMaxPasswordAttempts(int maxPasswordAttempts) {
    this.maxPasswordAttempts = maxPasswordAttempts;
  }
}
