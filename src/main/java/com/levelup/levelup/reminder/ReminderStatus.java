package com.levelup.levelup.reminder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ReminderStatus {
  PENDING("pending"),
  SENT("sent"),
  FAILED("failed"),
  CANCELLED("cancelled");

  private static final Map<String, ReminderStatus> BY_VALUE =
      Arrays.stream(values()).collect(Collectors.toMap(ReminderStatus::value, Function.identity()));

  private final String value;

  ReminderStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static ReminderStatus from(String value) {
    ReminderStatus status = value == null ? null : BY_VALUE.get(value.trim().toLowerCase());
    if (status == null) {
      throw new IllegalArgumentException(
          "Estado de recordatorio inválido: usa pending, sent, failed o cancelled.");
    }
    return status;
  }
}
