package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.NotificationEvent;
import org.example.jaipark_back.entity.Notification;
import org.example.jaipark_back.entity.User;
import org.example.jaipark_back.repository.NotificationRepository;
import org.example.jaipark_back.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

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

    public List<Notification> getNotifications(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        if (!notification.getUser().getUsername().equals(username)) throw new RuntimeException("권한 없음");
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public long countUnread(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
} 