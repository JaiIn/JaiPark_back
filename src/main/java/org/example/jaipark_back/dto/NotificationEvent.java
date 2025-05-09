package org.example.jaipark_back.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationEvent {
    private String username; // 알림 수신자
    private String type; // COMMENT, FOLLOW 등
    private String message;
    private Long postId; // 관련 게시글 ID(댓글 알림 등)
} 