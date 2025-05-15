package org.example.jaipark_back.service;

import lombok.RequiredArgsConstructor;
import org.example.jaipark_back.dto.ChatEvent;
import org.example.jaipark_back.dto.ChatMessageDto;
import org.example.jaipark_back.dto.ChatRoomDto;
import org.example.jaipark_back.entity.ChatMessage;
import org.example.jaipark_back.entity.ChatRoom;
import org.example.jaipark_back.entity.User;
import org.example.jaipark_back.repository.ChatMessageRepository;
import org.example.jaipark_back.repository.ChatRoomRepository;
import org.example.jaipark_back.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;
    
    // 실시간 사용자 온라인 상태 관리
    private final Map<String, Boolean> userOnlineStatus = new ConcurrentHashMap<>();
    
    private static final String CHAT_MESSAGE_TOPIC = "chat-message";
    private static final String CHAT_READ_TOPIC = "chat-read";
    private static final String CHAT_TYPING_TOPIC = "chat-typing";
    private static final String CHAT_STATUS_TOPIC = "chat-status";
    
    /**
     * 채팅 메시지 저장 및 전송
     */
    @Transactional
    public ChatMessageDto sendMessage(ChatMessageDto messageDto) {
        logger.info("Sending message: {}", messageDto);
        
        // 1. 채팅 메시지 저장
        final ChatMessage chatMessage = messageDto.toEntity();
        final ChatMessage savedChatMessage = chatMessageRepository.save(chatMessage);
        
        // 2. 채팅방 생성 또는 업데이트
        String chatRoomId = savedChatMessage.getChatRoomId();
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseGet(() -> {
                    // 새 채팅방 생성
                    return chatRoomRepository.save(
                            new ChatRoom(savedChatMessage.getSenderId(), savedChatMessage.getReceiverId())
                    );
                });
        
        // 최근 메시지 시간 업데이트
        chatRoom.setLastMessageTime(savedChatMessage.getTimestamp());
        chatRoomRepository.save(chatRoom);
        
        // 3. 상대방에게 메시지 전송 (Kafka)
        ChatMessageDto savedDto = ChatMessageDto.fromEntity(savedChatMessage);
        ChatEvent chatEvent = ChatEvent.messageEvent(
                savedChatMessage.getSenderId(), 
                savedChatMessage.getReceiverId(), 
                savedDto
        );
        
        // 수신자를 키로 사용하여 해당 사용자만 구독하도록
        kafkaTemplate.send(CHAT_MESSAGE_TOPIC, savedChatMessage.getReceiverId(), chatEvent);
        
        return savedDto;
    }
    
    /**
     * 메시지 읽음 표시 및 알림
     */
    @Transactional
    public void markMessagesAsRead(String chatRoomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        // 가장 최근 메시지 찾기
        ChatMessage latestMessage = chatMessageRepository.findTopByChatRoomIdOrderByTimestampDesc(chatRoomId);
        if (latestMessage == null) {
            return;
        }
        
        // 채팅방의 상대방 유저 ID 찾기
        String otherUserId = chatRoom.getUser1Id().equals(userId) ? chatRoom.getUser2Id() : chatRoom.getUser1Id();
        
        // 읽지 않은 메시지 조회 및 업데이트
        Pageable pageable = PageRequest.of(0, 100); // 한 번에 최대 100개 처리
        Page<ChatMessage> messagesPage = chatMessageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId, pageable);
        
        List<ChatMessage> unreadMessages = messagesPage.getContent().stream()
                .filter(message -> message.getReceiverId().equals(userId) && !message.isRead())
                .collect(Collectors.toList());
        
        List<ChatMessage> updatedMessages = new ArrayList<>();
        unreadMessages.forEach(message -> {
            message.setRead(true);
            updatedMessages.add(message);
        });
        
        if (!updatedMessages.isEmpty()) {
            chatMessageRepository.saveAll(updatedMessages);
            
            // 채팅방의 마지막 읽은 메시지 ID 업데이트
            chatRoom.updateLastReadMessageId(userId, latestMessage.getId());
            chatRoomRepository.save(chatRoom);
            
            // 상대방에게 읽음 이벤트 전송
            ChatEvent readEvent = ChatEvent.readEvent(userId, otherUserId, chatRoomId, latestMessage.getId());
            kafkaTemplate.send(CHAT_READ_TOPIC, otherUserId, readEvent);
        }
    }
    
    /**
     * 채팅방 메시지 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getChatMessages(String chatRoomId, String userId, int page, int size) {
        // 1. 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        // 2. 사용자가 채팅방의 참여자인지 확인
        if (!chatRoom.getUser1Id().equals(userId) && !chatRoom.getUser2Id().equals(userId)) {
            throw new RuntimeException("해당 채팅방에 대한 접근 권한이 없습니다.");
        }
        
        // 3. 메시지 조회 (최신 메시지 순으로)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId, pageable);
        
        // 4. Entity -> DTO 변환
        return messages.map(ChatMessageDto::fromEntity);
    }
    
    /**
     * 사용자의 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getChatRooms(String userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByUserId(userId);
        List<ChatRoomDto> chatRoomDtos = new ArrayList<>();
        
        for (ChatRoom chatRoom : chatRooms) {
            // 상대방 ID 확인
            String otherUserId = chatRoom.getUser1Id().equals(userId) ? 
                    chatRoom.getUser2Id() : chatRoom.getUser1Id();
            
            // 상대방 정보 조회
            User otherUser = userRepository.findByUsername(otherUserId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 최신 메시지 조회
            ChatMessage latestMessage = chatMessageRepository.findTopByChatRoomIdOrderByTimestampDesc(chatRoom.getId());
            String lastMessageContent = latestMessage != null ? latestMessage.getContent() : "";
            
            // 안 읽은 메시지 수 조회
            int unreadCount = chatMessageRepository.countUnreadMessagesInChatRoom(chatRoom.getId(), userId);
            
            // DTO 생성
            ChatRoomDto chatRoomDto = ChatRoomDto.fromEntity(
                    chatRoom, 
                    userId, 
                    lastMessageContent, 
                    unreadCount, 
                    otherUser.getNickname(),
                    otherUser.getProfileImage()
            );
            
            chatRoomDtos.add(chatRoomDto);
        }
        
        return chatRoomDtos;
    }
    
    /**
     * 채팅방 찾기 또는 생성
     */
    @Transactional
    public ChatRoom findOrCreateChatRoom(String user1Id, String user2Id) {
        // 두 사용자 모두 존재하는지 확인
        userRepository.findByUsername(user1Id)
                .orElseThrow(() -> new RuntimeException("첫 번째 사용자를 찾을 수 없습니다."));
        userRepository.findByUsername(user2Id)
                .orElseThrow(() -> new RuntimeException("두 번째 사용자를 찾을 수 없습니다."));
        
        // 기존 채팅방 찾기
        String chatRoomId = ChatMessage.generateChatRoomId(user1Id, user2Id);
        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findById(chatRoomId);
        
        if (existingChatRoom.isPresent()) {
            return existingChatRoom.get();
        }
        
        // 새로운 채팅방 생성
        ChatRoom newChatRoom = new ChatRoom(user1Id, user2Id);
        return chatRoomRepository.save(newChatRoom);
    }
    
    /**
     * 사용자 온라인 상태 업데이트
     */
    @Async
    public void updateUserOnlineStatus(String userId, boolean isOnline) {
        userOnlineStatus.put(userId, isOnline);
        
        // 온라인 상태 이벤트 생성 및 전송
        ChatEvent statusEvent = ChatEvent.onlineStatusEvent(userId, isOnline);
        
        // 모든 사용자의 채팅방을 찾아서 상대방에게 알림
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByUserId(userId);
        for (ChatRoom chatRoom : chatRooms) {
            String otherUserId = chatRoom.getUser1Id().equals(userId) ? 
                    chatRoom.getUser2Id() : chatRoom.getUser1Id();
            
            // 상대방에게 온라인 상태 전송
            kafkaTemplate.send(CHAT_STATUS_TOPIC, otherUserId, statusEvent);
        }
    }
    
    /**
     * 사용자 타이핑 상태 전송
     */
    public void sendTypingStatus(String senderId, String receiverId, boolean isTyping) {
        String chatRoomId = ChatMessage.generateChatRoomId(senderId, receiverId);
        ChatEvent typingEvent = ChatEvent.typingEvent(senderId, receiverId, chatRoomId, isTyping);
        kafkaTemplate.send(CHAT_TYPING_TOPIC, receiverId, typingEvent);
    }
    
    /**
     * 안 읽은 전체 메시지 수 조회
     */
    @Transactional(readOnly = true)
    public int getTotalUnreadMessageCount(String userId) {
        return chatMessageRepository.countUnreadMessagesByReceiverId(userId);
    }
    
    /**
     * 특정 채팅방의 안 읽은 메시지 수 조회
     */
    @Transactional(readOnly = true)
    public int getUnreadMessageCount(String chatRoomId, String userId) {
        return chatMessageRepository.countUnreadMessagesInChatRoom(chatRoomId, userId);
    }
}
