package com.levelup.levelup.notification;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.common.PageResponse;
import com.levelup.levelup.notification.dto.NotificationResponse;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private static final int MAX_PAGE_SIZE = 100;

  private final NotificationRepository notificationRepository;
  private final Clock clock;

  public NotificationService(NotificationRepository notificationRepository, Clock clock) {
    this.notificationRepository = notificationRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResponse<NotificationResponse> list(
      UUID userId, boolean unreadOnly, int page, int size) {
    Pageable pageable =
        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    Page<Notification> result =
        unreadOnly
            ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(
                userId, pageable)
            : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    return PageResponse.from(result, NotificationMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public long unreadCount(UUID userId) {
    return notificationRepository.countByUserIdAndReadAtIsNull(userId);
  }

  @Transactional
  public NotificationResponse markRead(UUID userId, UUID notificationId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> ApiException.notFound("Notificación no encontrada."));
    if (!notification.isRead()) {
      notification.markRead(clock.instant());
      notificationRepository.save(notification);
    }
    return NotificationMapper.toResponse(notification);
  }

  @Transactional
  public int markAllRead(UUID userId) {
    return notificationRepository.markAllRead(userId, clock.instant());
  }
}
