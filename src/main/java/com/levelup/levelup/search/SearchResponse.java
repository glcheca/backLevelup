package com.levelup.levelup.search;

import java.util.List;

public record SearchResponse(
    List<SearchItem> courses,
    List<SearchItem> events,
    List<SearchItem> tasks,
    List<SearchItem> notes) {

  public static SearchResponse empty() {
    return new SearchResponse(List.of(), List.of(), List.of(), List.of());
  }
}
