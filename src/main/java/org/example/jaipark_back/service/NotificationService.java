package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.NotificationEvent;
import org.example.jaipark_back.entity.Notification;
import org.example.jaipark_back.entity.User;
import org.example.jaipark_back.repository.NotificationRepository;
import org.example.jaipark_back.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 알림 이벤트를 비동기적으로 처리해 저장합니다.
     * @param event 알림 이벤트
     * @return CompletableFuture<Void>
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public CompletableFuture<Void> saveNotificationAsync(NotificationEvent event) {
        try {
            User user = userRepository.findByUsername(event.getUsername()).orElseThrow();
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(event.getType());
            notification.setMessage(event.getMessage());
            notification.setPostId(event.getPostId());
            notificationRepository.save(notification);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 다수의 알림을 배치로 처리합니다.
     * @param events 알림 이벤트 목록
     * @return CompletableFuture<Void>
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> saveNotificationBatch(List<NotificationEvent> events) {
        try {
            List<Notification> notifications = new ArrayList<>();
            
            for (NotificationEvent event : events) {
                User user = userRepository.findByUsername(event.getUsername()).orElseThrow();
                Notification notification = new Notification();
                notification.setUser(user);
                notification.setType(event.getType());
                notification.setMessage(event.getMessage());
                notification.setPostId(event.getPostId());
                notifications.add(notification);
            }
            
            notificationRepository.saveAll(notifications);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 동기 방식의 알림 저장 (Kafka Consumer에서 사용)
     */
    @Transactional
    public void saveNotification(NotificationEvent event) {
        User user = userRepository.findByUsername(event.getUsername()).orElseThrow();
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(event.getType());
        notification.setMessage(event.getMessage());
        notification.setPostId(event.getPostId());
        notificationRepository.save(notification);
    }

    /**
     * 사용자의 모든 알림을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 특정 알림을 읽음 상태로 표시합니다.
     */
    @Transactional
    public void markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        if (!notification.getUser().getUsername().equals(username)) throw new RuntimeException("권한 없음");
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * 사용자의 모든 알림을 읽음 상태로 표시합니다.
     */
    @Async
    @Transactional
    public CompletableFuture<Void> markAllAsRead(String username) {
        try {
            User user = userRepository.findByUsername(username).orElseThrow();
            List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalse(user);
            
            for (Notification notification : unreadNotifications) {
                notification.setRead(true);
            }
            
            notificationRepository.saveAll(unreadNotifications);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 읽지 않은 알림 개수를 조회합니다.
     */
    @Transactional(readOnly = true)
    public long countUnread(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
}