package org.example.jaipark_back.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    private String nickname;
    private String email;
    private String profileImage;
} 