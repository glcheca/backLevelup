package com.levelup.levelup.note;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.note.dto.NotePinRequest;
import com.levelup.levelup.note.dto.NoteRequest;
import com.levelup.levelup.note.dto.NoteResponse;
import com.levelup.levelup.note.dto.NoteSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
    this.noteService = noteService;
  }

  @GetMapping
  public PageResponse<NoteSummaryResponse> list(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @RequestParam(required = false) UUID courseId,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean pinned,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    return noteService.list(principal.id(), courseId, tag, q, pinned, page, size);
  }

  @GetMapping("/tags")
  public List<String> tags(@AuthenticationPrincipal AuthenticatedUser principal) {
    return noteService.tags(principal.id());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NoteResponse create(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody NoteRequest request) {
    return noteService.create(principal.id(), request);
  }

  @GetMapping("/{noteId}")
  public NoteResponse get(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID noteId) {
    return noteService.get(principal.id(), noteId);
  }

  @PatchMapping("/{noteId}")
  public NoteResponse update(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID noteId,
      @Valid @RequestBody NoteRequest request) {
    return noteService.update(principal.id(), noteId, request);
  }

  @PatchMapping("/{noteId}/pin")
  public NoteResponse pin(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID noteId,
      @Valid @RequestBody NotePinRequest request) {
    return noteService.setPinned(principal.id(), noteId, request.pinned());
  }

  @DeleteMapping("/{noteId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID noteId) {
    noteService.delete(principal.id(), noteId);
  }
}
