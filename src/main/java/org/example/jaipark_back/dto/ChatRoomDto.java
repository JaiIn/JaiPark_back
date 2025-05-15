package org.example.jaipark_back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.jaipark_back.entity.ChatRoom;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    private String id;
    private String otherUserId;
    private String otherUserNickname;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
    private String profileImage;
    
    // ChatRoom 엔티티와 추가 데이터로부터 DTO 생성
    public static ChatRoomDto fromEntity(ChatRoom chatRoom, String currentUserId, String lastMessage, 
                                        int unreadCount, String otherUserNickname, String profileImage) {
        ChatRoomDto dto = new ChatRoomDto();
        dto.setId(chatRoom.getId());
        
        // 상대방 ID 설정
        String otherUserId = chatRoom.getUser1Id().equals(currentUserId) 
                             ? chatRoom.getUser2Id() : chatRoom.getUser1Id();
        dto.setOtherUserId(otherUserId);
        
        dto.setOtherUserNickname(otherUserNickname);
        dto.setLastMessage(lastMessage);
        dto.setLastMessageTime(chatRoom.getLastMessageTime());
        dto.setUnreadCount(unreadCount);
        dto.setProfileImage(profileImage);
        
        return dto;
    }
}
