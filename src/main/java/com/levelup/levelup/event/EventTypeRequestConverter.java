package com.levelup.levelup.event;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Permite usar los valores de la API (`exam`, `assignment`, `class`, `personal`) también en los
 * parámetros de consulta, como en `GET /events?type=exam`.
 */
@Component
public class EventTypeRequestConverter implements Converter<String, EventType> {

  @Override
  public EventType convert(String source) {
    return EventType.from(source);
  }
}
