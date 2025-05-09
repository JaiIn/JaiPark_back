package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.Notification;
import org.example.jaipark_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndIsReadFalse(User user);
} 