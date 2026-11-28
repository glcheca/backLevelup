package com.levelup.levelup.reminder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A qué apunta un recordatorio. Los valores de la API/BD van en minúsculas. */
public enum ReminderTargetType {
  TASK("task"),
  EVENT("event"),
  CUSTOM("custom");

  private static final Map<String, ReminderTargetType> BY_VALUE =
      Arrays.stream(values())
          .collect(Collectors.toMap(ReminderTargetType::value, Function.identity()));

  private final String value;

  ReminderTargetType(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static ReminderTargetType from(String value) {
    ReminderTargetType type = value == null ? null : BY_VALUE.get(value.trim().toLowerCase());
    if (type == null) {
      throw new IllegalArgumentException(
          "Tipo de recordatorio inválido: usa task, event o custom.");
    }
    return type;
  }
}
