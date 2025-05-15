package org.example.jaipark_back.service;

import lombok.RequiredArgsConstructor;
import org.example.jaipark_back.dto.ChatEvent;
import org.example.jaipark_back.dto.ChatMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ChatProducer {
    private static final Logger logger = LoggerFactory.getLogger(ChatProducer.class);
    
    private static final String CHAT_MESSAGE_TOPIC = "chat-message";
    private static final String CHAT_READ_TOPIC = "chat-read";
    private static final String CHAT_TYPING_TOPIC = "chat-typing";
    private static final String CHAT_STATUS_TOPIC = "chat-status";
    
    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;
    
    /**
     * 채팅 메시지 이벤트 전송
     */
    @Async
    public CompletableFuture<SendResult<String, ChatEvent>> sendChatMessage(ChatMessageDto messageDto) {
        ChatEvent event = ChatEvent.messageEvent(
                messageDto.getSenderId(),
                messageDto.getReceiverId(),
                messageDto
        );
        
        return kafkaTemplate.send(CHAT_MESSAGE_TOPIC, messageDto.getReceiverId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Chat message sent successfully: {}", result.getRecordMetadata());
                    } else {
                        logger.error("Error sending chat message: {}", ex.getMessage(), ex);
                    }
                });
    }
    
    /**
     * 읽음 확인 이벤트 전송
     */
    @Async
    public CompletableFuture<SendResult<String, ChatEvent>> sendReadReceipt(String senderId, String receiverId,
                                                      String chatRoomId, Long messageId) {
        ChatEvent event = ChatEvent.readEvent(senderId, receiverId, chatRoomId, messageId);
        
        return kafkaTemplate.send(CHAT_READ_TOPIC, receiverId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Read receipt sent successfully: {}", result.getRecordMetadata());
                    } else {
                        logger.error("Error sending read receipt: {}", ex.getMessage(), ex);
                    }
                });
    }
    
    /**
     * 타이핑 상태 이벤트 전송
     */
    @Async
    public CompletableFuture<SendResult<String, ChatEvent>> sendTypingStatus(String senderId, String receiverId,
                                                       String chatRoomId, boolean isTyping) {
        ChatEvent event = ChatEvent.typingEvent(senderId, receiverId, chatRoomId, isTyping);
        
        return kafkaTemplate.send(CHAT_TYPING_TOPIC, receiverId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Typing status sent successfully: {}", result.getRecordMetadata());
                    } else {
                        logger.error("Error sending typing status: {}", ex.getMessage(), ex);
                    }
                });
    }
    
    /**
     * 온라인 상태 이벤트 전송
     */
    @Async
    public CompletableFuture<SendResult<String, ChatEvent>> sendOnlineStatus(String userId, boolean isOnline) {
        ChatEvent event = ChatEvent.onlineStatusEvent(userId, isOnline);
        
        // 모든 사용자에게 전송 (특정 토픽으로)
        return kafkaTemplate.send(CHAT_STATUS_TOPIC, "all-users", event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Online status sent successfully: {}", result.getRecordMetadata());
                    } else {
                        logger.error("Error sending online status: {}", ex.getMessage(), ex);
                    }
                });
    }
}
