package com.levelup.levelup.notification;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.notification.dto.NotificationCountResponse;
import com.levelup.levelup.notification.dto.NotificationResponse;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public PageResponse<NotificationResponse> list(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @RequestParam(defaultValue = "false") boolean unreadOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return notificationService.list(principal.id(), unreadOnly, page, size);
  }

  @GetMapping("/unread-count")
  public NotificationCountResponse unreadCount(
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return new NotificationCountResponse(notificationService.unreadCount(principal.id()));
  }

  @PatchMapping("/{notificationId}/read")
  public NotificationResponse markRead(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID notificationId) {
    return notificationService.markRead(principal.id(), notificationId);
  }

  @PatchMapping("/read-all")
  public NotificationCountResponse markAllRead(
      @AuthenticationPrincipal AuthenticatedUser principal) {
    notificationService.markAllRead(principal.id());
    return new NotificationCountResponse(notificationService.unreadCount(principal.id()));
  }
}
