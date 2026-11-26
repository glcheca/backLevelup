package com.levelup.levelup.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Envoltura estable para las respuestas paginadas (la serialización de `Page` de Spring no es
 * estable entre versiones).
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> mapper) {
    return new PageResponse<>(
        source.getContent().stream().map(mapper).toList(),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages());
  }
}
