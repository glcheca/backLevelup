package com.levelup.levelup.reminder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReminderTargetTypeConverter implements AttributeConverter<ReminderTargetType, String> {

  @Override
  public String convertToDatabaseColumn(ReminderTargetType attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public ReminderTargetType convertToEntityAttribute(String dbData) {
    return dbData == null ? null : ReminderTargetType.from(dbData);
  }
}
