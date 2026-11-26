package com.levelup.levelup.user.dto;

import com.levelup.levelup.user.User;
import java.util.UUID;

public record UserResponse(
    UUID id, String email, String name, String timezone, String locale, String theme) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.getTimezone(),
        user.getLocale(),
        user.getTheme());
  }
}
