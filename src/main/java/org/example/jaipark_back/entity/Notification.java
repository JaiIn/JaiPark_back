package org.example.jaipark_back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 알림 수신자

    @Column(nullable = false)
    private String type; // COMMENT, FOLLOW 등

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private Long postId; // 관련 게시글 ID(댓글 알림 등)

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 