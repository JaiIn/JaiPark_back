package org.example.jaipark_back.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String username;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> comments;
} 