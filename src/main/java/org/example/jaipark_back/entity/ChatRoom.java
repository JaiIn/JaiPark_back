package org.example.jaipark_back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    
    @Id
    @Column(nullable = false)
    private String id;
    
    @Column(nullable = false)
    private String user1Id;
    
    @Column(nullable = false)
    private String user2Id;
    
    @Column(nullable = false)
    private LocalDateTime lastMessageTime = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // 사용자별 마지막으로 읽은 메시지 ID
    @Column(name = "user1_last_read_message_id")
    private Long user1LastReadMessageId;
    
    @Column(name = "user2_last_read_message_id")
    private Long user2LastReadMessageId;
    
    // 생성자 오버로딩
    public ChatRoom(String user1Id, String user2Id) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.id = ChatMessage.generateChatRoomId(user1Id, user2Id);
        this.createdAt = LocalDateTime.now();
        this.lastMessageTime = LocalDateTime.now();
    }
    
    // 특정 사용자의 마지막으로 읽은 메시지 ID 업데이트
    public void updateLastReadMessageId(String userId, Long messageId) {
        if (userId.equals(user1Id)) {
            this.user1LastReadMessageId = messageId;
        } else if (userId.equals(user2Id)) {
            this.user2LastReadMessageId = messageId;
        }
    }
    
    // 특정 사용자의 마지막으로 읽은 메시지 ID 가져오기
    public Long getLastReadMessageId(String userId) {
        if (userId.equals(user1Id)) {
            return user1LastReadMessageId;
        } else if (userId.equals(user2Id)) {
            return user2LastReadMessageId;
        }
        return null;
    }
}
