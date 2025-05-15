package org.example.jaipark_back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatEvent {
    private String type;          // "MESSAGE", "READ", "TYPING", "ONLINE", "OFFLINE"
    private String senderId;      // 발신자 ID
    private String receiverId;    // 수신자 ID
    private String chatRoomId;    // 채팅방 ID
    private Object data;          // 이벤트 데이터 (메시지 등)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // 메시지 이벤트 생성 헬퍼 메소드
    public static ChatEvent messageEvent(String senderId, String receiverId, ChatMessageDto message) {
        ChatEvent event = new ChatEvent();
        event.setType("MESSAGE");
        event.setSenderId(senderId);
        event.setReceiverId(receiverId);
        event.setChatRoomId(message.getChatRoomId());
        event.setData(message);
        return event;
    }
    
    // 읽음 확인 이벤트 생성 헬퍼 메소드
    public static ChatEvent readEvent(String senderId, String receiverId, String chatRoomId, Long messageId) {
        ChatEvent event = new ChatEvent();
        event.setType("READ");
        event.setSenderId(senderId);
        event.setReceiverId(receiverId);
        event.setChatRoomId(chatRoomId);
        event.setData(messageId);
        return event;
    }
    
    // 타이핑 이벤트 생성 헬퍼 메소드
    public static ChatEvent typingEvent(String senderId, String receiverId, String chatRoomId, boolean isTyping) {
        ChatEvent event = new ChatEvent();
        event.setType("TYPING");
        event.setSenderId(senderId);
        event.setReceiverId(receiverId);
        event.setChatRoomId(chatRoomId);
        event.setData(isTyping);
        return event;
    }
    
    // 온라인 상태 이벤트 생성 헬퍼 메소드
    public static ChatEvent onlineStatusEvent(String userId, boolean isOnline) {
        ChatEvent event = new ChatEvent();
        event.setType(isOnline ? "ONLINE" : "OFFLINE");
        event.setSenderId(userId);
        event.setData(isOnline);
        return event;
    }
}
