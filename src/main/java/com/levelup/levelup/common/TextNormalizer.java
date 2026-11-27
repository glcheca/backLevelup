package com.levelup.levelup.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza texto para búsquedas insensibles a mayúsculas y tildes.
 *
 * <p>Se usa en lugar de `tsvector` para que el esquema y las consultas funcionen igual en
 * PostgreSQL y en H2 (tests). Para el volumen del MVP es suficiente; si crece, se puede migrar a
 * full-text de PostgreSQL sin cambiar la API.
 */
public final class TextNormalizer {

  private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private TextNormalizer() {}

  public static String normalize(String value) {
    if (value == null) {
      return "";
    }
    String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
    return WHITESPACE
        .matcher(DIACRITICS.matcher(decomposed).replaceAll(""))
        .replaceAll(" ")
        .toLowerCase(Locale.ROOT)
        .trim();
  }
}
