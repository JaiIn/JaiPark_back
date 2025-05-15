package org.example.jaipark_back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String senderId; // 보낸 사람의 username
    
    @Column(nullable = false)
    private String receiverId; // 받는 사람의 username
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type = MessageType.TEXT;
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(nullable = false)
    private boolean isRead = false;
    
    // 채팅방 식별자 (sender와 receiver로 구성)
    @Column(nullable = false)
    private String chatRoomId;
    
    // 메시지 타입 열거형
    public enum MessageType {
        TEXT,       // 일반 텍스트 메시지
        IMAGE,      // 이미지 메시지
        FILE,       // 파일 메시지
        SYSTEM      // 시스템 메시지
    }
    
    // 채팅방 ID 생성 (사용자 ID 알파벳 순으로 정렬하여 일관성 유지)
    public static String generateChatRoomId(String userId1, String userId2) {
        return userId1.compareTo(userId2) <= 0 
               ? userId1 + "_" + userId2 
               : userId2 + "_" + userId1;
    }
    
    // 생성자 오버로딩
    public ChatMessage(String senderId, String receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.chatRoomId = generateChatRoomId(senderId, receiverId);
    }
}
