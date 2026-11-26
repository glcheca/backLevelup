package com.levelup.levelup.task;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guarda las subtareas como JSON en una columna de texto.
 *
 * <p>Se usa texto (y no `jsonb`) para que el esquema generado sea idéntico en PostgreSQL y en H2
 * (perfil de tests). La validación del contenido la hace {@link TaskService}.
 */
@Converter
public class SubtaskListConverter implements AttributeConverter<List<Subtask>, String> {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final TypeReference<List<Subtask>> TYPE = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(List<Subtask> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "[]";
    }
    try {
      return MAPPER.writeValueAsString(attribute);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudieron serializar las subtareas", exception);
    }
  }

  @Override
  public List<Subtask> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return new ArrayList<>();
    }
    try {
      return MAPPER.readValue(dbData, TYPE);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudieron leer las subtareas", exception);
    }
  }
}
