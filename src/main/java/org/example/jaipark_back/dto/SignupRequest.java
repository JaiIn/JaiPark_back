package org.example.jaipark_back.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String username;
    private String password;
    private String email;
    private String nickname;
    private String gender;
    private String birth;
} 