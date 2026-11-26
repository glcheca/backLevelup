package com.levelup.levelup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tipo de evento.
 *
 * <p>`class` es palabra reservada en Java, así que las constantes van en mayúsculas y se traducen a
 * los valores de la API/BD (`exam`, `assignment`, `class`, `personal`) con {@link #value()} y
 * {@link #from(String)}, y con {@link EventTypeConverter} al persistir.
 */
public enum EventType {
  EXAM("exam"),
  ASSIGNMENT("assignment"),
  CLASS("class"),
  PERSONAL("personal");

  private static final Map<String, EventType> BY_VALUE =
      Arrays.stream(values()).collect(Collectors.toMap(EventType::value, Function.identity()));

  private final String value;

  EventType(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static EventType from(String value) {
    EventType type = value == null ? null : BY_VALUE.get(value.trim().toLowerCase());
    if (type == null) {
      throw new IllegalArgumentException(
          "Tipo de evento inválido: usa exam, assignment, class o personal.");
    }
    return type;
  }
}
