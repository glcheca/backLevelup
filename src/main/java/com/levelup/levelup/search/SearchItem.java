package com.levelup.levelup.search;

import java.util.UUID;

/** Un resultado de la búsqueda global. */
public record SearchItem(UUID id, String title, String subtitle, String type) {}
