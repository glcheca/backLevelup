package com.levelup.levelup.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Textos de los recordatorios (fecha/hora en la zona del usuario). */
public final class ReminderText {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.forLanguageTag("es"));

  private ReminderText() {}

  public static String body(String prefix, Instant when, ZoneId zone, String extra) {
    String formatted = FORMATTER.withZone(zone).format(when);
    return extra == null || extra.isBlank()
        ? "%s %s".formatted(prefix, formatted)
        : "%s %s · %s".formatted(prefix, formatted, extra);
  }
}
