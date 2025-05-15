package org.example.jaipark_back.controller;

import lombok.RequiredArgsConstructor;
import org.example.jaipark_back.dto.UserUpdateRequest;
import org.example.jaipark_back.dto.PasswordChangeRequest;
import org.example.jaipark_back.dto.UserResponse;
import org.example.jaipark_back.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(userService.getMe(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@RequestBody UserUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(userService.updateMe(request, authentication.getName()));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request, Authentication authentication) {
        userService.changePassword(request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getPublicProfile(username));
    }

    // 팔로우
    @PostMapping("/{username}/follow")
    public ResponseEntity<?> follow(@PathVariable String username, Authentication authentication) {
        userService.follow(authentication.getName(), username);
        return ResponseEntity.ok().build();
    }

    // 언팔로우
    @DeleteMapping("/{username}/follow")
    public ResponseEntity<?> unfollow(@PathVariable String username, Authentication authentication) {
        userService.unfollow(authentication.getName(), username);
        return ResponseEntity.ok().build();
    }

    // 팔로잉 목록
    @GetMapping("/{username}/following")
    public ResponseEntity<?> getFollowing(@PathVariable String username) {
        return ResponseEntity.ok(userService.getFollowing(username));
    }

    // 팔로워 목록
    @GetMapping("/{username}/followers")
    public ResponseEntity<?> getFollowers(@PathVariable String username) {
        return ResponseEntity.ok(userService.getFollowers(username));
    }

    // 팔로우 여부 및 카운트
    @GetMapping("/{username}/follow-status")
    public ResponseEntity<?> getFollowStatus(@PathVariable String username, Authentication authentication) {
        String me = authentication != null ? authentication.getName() : null;
        boolean isFollowing = false;
        if (me != null) {
            isFollowing = userService.isFollowing(me, username);
        }
        long followingCount = userService.countFollowing(username);
        long followerCount = userService.countFollowers(username);
        return ResponseEntity.ok(java.util.Map.of(
            "isFollowing", isFollowing,
            "followingCount", followingCount,
            "followerCount", followerCount
        ));
    }
} 