package com.levelup.levelup.reminder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReminderStatusConverter implements AttributeConverter<ReminderStatus, String> {

  @Override
  public String convertToDatabaseColumn(ReminderStatus attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public ReminderStatus convertToEntityAttribute(String dbData) {
    return dbData == null ? null : ReminderStatus.from(dbData);
  }
}
