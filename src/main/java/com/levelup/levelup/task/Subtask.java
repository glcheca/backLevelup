package com.levelup.levelup.task;

/** Elemento de la lista de comprobación de una tarea. Se guarda como JSON en `task`. */
public record Subtask(String id, String title, boolean done) {}
