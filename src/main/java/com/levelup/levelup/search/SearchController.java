package com.levelup.levelup.search;

import com.levelup.levelup.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

  private final SearchService searchService;

  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping
  public SearchResponse search(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @RequestParam(required = false) String q) {
    return searchService.search(principal.id(), q);
  }
}
