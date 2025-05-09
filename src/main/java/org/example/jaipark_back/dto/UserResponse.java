package org.example.jaipark_back.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String profileImage;
} 