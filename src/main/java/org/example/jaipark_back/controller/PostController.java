package org.example.jaipark_back.controller;

import org.example.jaipark_back.dto.PostRequest;
import org.example.jaipark_back.dto.PostResponse;
import org.example.jaipark_back.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {
    @Autowired
    private PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest request, Authentication authentication) {
        return ResponseEntity.ok(postService.createPost(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(Pageable pageable) {
        return ResponseEntity.ok(postService.getAllPosts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id, @RequestBody PostRequest request, Authentication authentication) {
        return ResponseEntity.ok(postService.updatePost(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, Authentication authentication) {
        postService.deletePost(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, Authentication authentication) {
        boolean liked = postService.toggleLike(id, authentication.getName());
        long count = postService.countLikes(id);
        return ResponseEntity.ok(Map.of("liked", liked, "count", count));
    }

    @GetMapping("/{id}/like")
    public ResponseEntity<?> isLiked(@PathVariable Long id, Authentication authentication) {
        boolean liked = postService.isLiked(id, authentication.getName());
        long count = postService.countLikes(id);
        return ResponseEntity.ok(Map.of("liked", liked, "count", count));
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<?> toggleBookmark(@PathVariable Long id, Authentication authentication) {
        boolean bookmarked = postService.toggleBookmark(id, authentication.getName());
        long count = postService.countBookmarks(id);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked, "count", count));
    }

    @GetMapping("/{id}/bookmark")
    public ResponseEntity<?> isBookmarked(@PathVariable Long id, Authentication authentication) {
        boolean bookmarked = postService.isBookmarked(id, authentication.getName());
        long count = postService.countBookmarks(id);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked, "count", count));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyPosts(Authentication authentication) {
        return ResponseEntity.ok(postService.getPostsByUsername(authentication.getName()));
    }

    @GetMapping("/liked")
    public ResponseEntity<?> getLikedPosts(Authentication authentication) {
        return ResponseEntity.ok(postService.getLikedPosts(authentication.getName()));
    }

    @GetMapping("/bookmarked")
    public ResponseEntity<?> getBookmarkedPosts(Authentication authentication) {
        return ResponseEntity.ok(postService.getBookmarkedPosts(authentication.getName()));
    }

    @GetMapping("/followings")
    public ResponseEntity<?> getFollowingsPosts(Authentication authentication) {
        return ResponseEntity.ok(postService.getFollowingsPosts(authentication.getName()));
    }
    
    /**
     * 키셋 페이지네이션을 이용한 게시물 목록 조회 API
     */
    @GetMapping("/cursor")
    public ResponseEntity<List<PostResponse>> getPostsWithCursor(
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(postService.getPostsWithCursor(lastPostId, limit));
    }
    
    /**
     * 시간 기반 키셋 페이지네이션 API
     */
    @GetMapping("/time-cursor")
    public ResponseEntity<List<PostResponse>> getPostsWithTimeCursor(
            @RequestParam(required = false) LocalDateTime createdAt,
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "20") int limit) {
        
        Map<String, Object> cursorParams = new HashMap<>();
        if (createdAt != null && id != null) {
            cursorParams.put("createdAt", createdAt);
            cursorParams.put("id", id);
        }
        
        return ResponseEntity.ok(postService.getPostsWithTimeCursor(cursorParams, limit));
    }
    
    /**
     * 팔로잉 게시물을 위한 키셋 페이지네이션 API
     */
    @GetMapping("/followings/cursor")
    public ResponseEntity<?> getFollowingsPostsWithCursor(
            Authentication authentication,
            @RequestParam(required = false) LocalDateTime createdAt,
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "20") int limit) {
        
        Map<String, Object> cursorParams = new HashMap<>();
        if (createdAt != null && id != null) {
            cursorParams.put("createdAt", createdAt);
            cursorParams.put("id", id);
        }
        
        return ResponseEntity.ok(postService.getFollowingsPostsWithCursor(
                authentication.getName(), cursorParams, limit));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPosts(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.searchPosts(keyword, page, size));
    }
} 