package org.example.jaipark_back.controller;

import lombok.RequiredArgsConstructor;
import org.example.jaipark_back.entity.Notification;
import org.example.jaipark_back.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {
    private final NotificationService notificationService;

    // 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(notificationService.getNotifications(username));
    }

    // 알림 읽음 처리
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        notificationService.markAsRead(id, username);
        return ResponseEntity.ok().build();
    }

    // 안읽은 알림 수
    @GetMapping("/unread-count")
    public ResponseEntity<Long> countUnread(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(notificationService.countUnread(username));
    }
} 