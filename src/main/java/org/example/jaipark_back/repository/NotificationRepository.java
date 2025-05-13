package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.Notification;
import org.example.jaipark_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    List<Notification> findByUserAndIsReadFalse(User user);
    
    long countByUserAndIsReadFalse(User user);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user")
    int markAllAsRead(@Param("user") User user);
    
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUser(@Param("user") User user);
    
    @Query(value = "SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Notification> findRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
}