package org.example.jaipark_back.service;

import lombok.RequiredArgsConstructor;
import org.example.jaipark_back.dto.SignupRequest;
import org.example.jaipark_back.entity.User;
import org.example.jaipark_back.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.example.jaipark_back.dto.UserUpdateRequest;
import org.example.jaipark_back.dto.PasswordChangeRequest;
import org.example.jaipark_back.dto.UserResponse;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import org.example.jaipark_back.entity.Follow;
import org.example.jaipark_back.repository.FollowRepository;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;

    public User createUser(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setNickname(signupRequest.getNickname());
        user.setGender(signupRequest.getGender());
        user.setBirth(signupRequest.getBirth());

        return userRepository.save(user);
    }

    @Transactional
    public UserResponse updateMe(UserUpdateRequest request, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getProfileImage() != null) user.setProfileImage(request.getProfileImage());
        userRepository.save(user);
        return convertToResponse(user);
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UserResponse getMe(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return convertToResponse(user);
    }

    public UserResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));
        return convertToResponse(user);
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setProfileImage(user.getProfileImage());
        response.setNickname(user.getNickname());
        // 팔로워/팔로잉 수
        response.setFollowerCount(followRepository.countByFollowing(user));
        response.setFollowingCount(followRepository.countByFollower(user));
        // 팔로워/팔로잉 목록
        response.setFollowers(followRepository.findByFollowing(user).stream().map(f -> convertToSimpleUserResponse(f.getFollower())).collect(java.util.stream.Collectors.toList()));
        response.setFollowings(followRepository.findByFollower(user).stream().map(f -> convertToSimpleUserResponse(f.getFollowing())).collect(java.util.stream.Collectors.toList()));
        return response;
    }

    // 팔로워/팔로잉 목록용(재귀 방지)
    private UserResponse convertToSimpleUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setProfileImage(user.getProfileImage());
        return response;
    }

    // 팔로우
    @Transactional
    public void follow(String followerUsername, String followingUsername) {
        if (followerUsername.equals(followingUsername)) throw new RuntimeException("자기 자신을 팔로우할 수 없습니다.");
        User follower = userRepository.findByUsername(followerUsername).orElseThrow();
        User following = userRepository.findByUsername(followingUsername).orElseThrow();
        if (followRepository.existsByFollowerAndFollowing(follower, following)) return;
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);
    }

    // 언팔로우
    @Transactional
    public void unfollow(String followerUsername, String followingUsername) {
        User follower = userRepository.findByUsername(followerUsername).orElseThrow();
        User following = userRepository.findByUsername(followingUsername).orElseThrow();
        followRepository.deleteByFollowerAndFollowing(follower, following);
    }

    // 팔로잉 목록
    public List<UserResponse> getFollowing(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return followRepository.findByFollower(user).stream()
            .map(f -> convertToResponse(f.getFollowing()))
            .collect(Collectors.toList());
    }

    // 팔로워 목록
    public List<UserResponse> getFollowers(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return followRepository.findByFollowing(user).stream()
            .map(f -> convertToResponse(f.getFollower()))
            .collect(Collectors.toList());
    }

    // 팔로우 여부
    public boolean isFollowing(String followerUsername, String followingUsername) {
        User follower = userRepository.findByUsername(followerUsername).orElseThrow();
        User following = userRepository.findByUsername(followingUsername).orElseThrow();
        return followRepository.existsByFollowerAndFollowing(follower, following);
    }

    // 팔로잉/팔로워 수
    public long countFollowing(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return followRepository.countByFollower(user);
    }
    public long countFollowers(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return followRepository.countByFollowing(user);
    }
} 