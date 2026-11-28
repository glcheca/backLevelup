package com.levelup.levelup.reminder;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Permite `GET /reminders?status=pending` con los valores de la API. */
@Component
public class ReminderStatusRequestConverter implements Converter<String, ReminderStatus> {

  @Override
  public ReminderStatus convert(String source) {
    return ReminderStatus.from(source);
  }
}
