package org.example.jaipark_back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.jaipark_back.entity.ChatMessage;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private String senderId;
    private String receiverId;
    private String content;
    private ChatMessage.MessageType type;
    private LocalDateTime timestamp;
    private boolean isRead;
    private String chatRoomId;
    
    // 생성자
    public ChatMessageDto(String senderId, String receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.type = ChatMessage.MessageType.TEXT;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
        this.chatRoomId = ChatMessage.generateChatRoomId(senderId, receiverId);
    }
    
    // Entity -> DTO 변환
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(entity.getId());
        dto.setSenderId(entity.getSenderId());
        dto.setReceiverId(entity.getReceiverId());
        dto.setContent(entity.getContent());
        dto.setType(entity.getType());
        dto.setTimestamp(entity.getTimestamp());
        dto.setRead(entity.isRead());
        dto.setChatRoomId(entity.getChatRoomId());
        return dto;
    }
    
    // DTO -> Entity 변환
    public ChatMessage toEntity() {
        ChatMessage entity = new ChatMessage();
        entity.setId(this.id);
        entity.setSenderId(this.senderId);
        entity.setReceiverId(this.receiverId);
        entity.setContent(this.content);
        entity.setType(this.type);
        entity.setTimestamp(this.timestamp);
        entity.setRead(this.isRead);
        entity.setChatRoomId(this.chatRoomId);
        return entity;
    }
}
