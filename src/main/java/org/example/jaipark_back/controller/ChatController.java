package org.example.jaipark_back.controller;

import lombok.RequiredArgsConstructor;
import org.example.jaipark_back.dto.ChatMessageDto;
import org.example.jaipark_back.dto.ChatRoomDto;
import org.example.jaipark_back.entity.ChatRoom;
import org.example.jaipark_back.service.ChatProducer;
import org.example.jaipark_back.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;
    private final ChatProducer chatProducer;
    
    /**
     * 채팅 목록 조회
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getChatRooms(Authentication authentication) {
        String userId = authentication.getName();
        List<ChatRoomDto> chatRooms = chatService.getChatRooms(userId);
        return ResponseEntity.ok(chatRooms);
    }
    
    /**
     * 채팅방 생성 또는 조회
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createOrGetChatRoom(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String currentUserId = authentication.getName();
        String otherUserId = request.get("userId");
        
        ChatRoom chatRoom = chatService.findOrCreateChatRoom(currentUserId, otherUserId);
        return ResponseEntity.ok(chatRoom);
    }
    
    /**
     * 채팅 메시지 조회
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageDto>> getChatMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        Page<ChatMessageDto> messages = chatService.getChatMessages(roomId, userId, page, size);
        
        // 메시지 읽음 표시
        chatService.markMessagesAsRead(roomId, userId);
        
        return ResponseEntity.ok(messages);
    }
    
    /**
     * 안 읽은 메시지 수 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<Integer> getUnreadMessageCount(Authentication authentication) {
        String userId = authentication.getName();
        int unreadCount = chatService.getTotalUnreadMessageCount(userId);
        return ResponseEntity.ok(unreadCount);
    }
    
    /**
     * 특정 채팅방의 안 읽은 메시지 수 조회
     */
    @GetMapping("/rooms/{roomId}/unread")
    public ResponseEntity<Integer> getUnreadMessageCountInRoom(
            @PathVariable String roomId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        int unreadCount = chatService.getUnreadMessageCount(roomId, userId);
        return ResponseEntity.ok(unreadCount);
    }
    
    /**
     * 메시지 읽음 표시
     */
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable String roomId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        chatService.markMessagesAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * WebSocket을 통한 채팅 메시지 전송
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessage, Principal principal) {
        String senderId = principal.getName();
        
        // 메시지 발신자 설정
        chatMessage.setSenderId(senderId);
        
        logger.info("Received message from {}: {}", senderId, chatMessage);
        
        // 메시지 저장 및 전송
        ChatMessageDto savedMessage = chatService.sendMessage(chatMessage);
        
        // Kafka를 통해 메시지 전송 (이벤트 기반 처리)
        chatProducer.sendChatMessage(savedMessage);
    }
    
    /**
     * WebSocket을 통한 타이핑 상태 전송
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> payload, Principal principal) {
        String senderId = principal.getName();
        String receiverId = (String) payload.get("receiverId");
        String chatRoomId = (String) payload.get("chatRoomId");
        boolean isTyping = (boolean) payload.get("isTyping");
        
        // Kafka를 통해 타이핑 상태 전송
        chatProducer.sendTypingStatus(senderId, receiverId, chatRoomId, isTyping);
    }
    
    /**
     * WebSocket을 통한 읽음 확인 전송
     */
    @MessageMapping("/chat.read")
    public void markRead(@Payload Map<String, Object> payload, Principal principal) {
        String senderId = principal.getName();
        String chatRoomId = (String) payload.get("chatRoomId");
        
        // 메시지 읽음 처리
        chatService.markMessagesAsRead(chatRoomId, senderId);
    }
    
    /**
     * WebSocket 연결 시 사용자 온라인 상태 업데이트
     */
    @MessageMapping("/chat.connect")
    public void connect(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        String userId = principal.getName();
        
        // 사용자 세션 정보 저장
        headerAccessor.getSessionAttributes().put("username", userId);
        
        // 사용자 온라인 상태 업데이트
        chatService.updateUserOnlineStatus(userId, true);
    }
    
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageDto> sendMessageRest(
            @RequestBody ChatMessageDto messageDto, 
            Authentication authentication) {
        
        String senderId = authentication.getName();
        messageDto.setSenderId(senderId);
        
        // 타입이 null인 경우 기본값 TEXT로 설정
        if (messageDto.getType() == null) {
            messageDto.setType(org.example.jaipark_back.entity.ChatMessage.MessageType.TEXT);
        }
        
        // 메시지 저장 및 전송
        ChatMessageDto savedMessage = chatService.sendMessage(messageDto);
        
        return ResponseEntity.ok(savedMessage);
    }
}
