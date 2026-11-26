package com.levelup.levelup.event;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Persiste el tipo de evento con su valor de API (`class`, no `CLASS`). */
@Converter(autoApply = true)
public class EventTypeConverter implements AttributeConverter<EventType, String> {

  @Override
  public String convertToDatabaseColumn(EventType attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public EventType convertToEntityAttribute(String dbData) {
    return dbData == null ? null : EventType.from(dbData);
  }
}
