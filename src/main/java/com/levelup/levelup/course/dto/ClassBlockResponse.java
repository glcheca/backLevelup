package com.levelup.levelup.course.dto;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Bloque de horario.
 *
 * @param warning aviso no bloqueante (por ejemplo, solapamiento con otro bloque del mismo curso);
 *     solo se rellena en las respuestas de creación y edición.
 */
public record ClassBlockResponse(
    UUID id,
    UUID courseId,
    String courseName,
    String courseColor,
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    String room,
    String warning) {}
