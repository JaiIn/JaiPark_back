package org.example.jaipark_back.controller;

import org.example.jaipark_back.dto.CommentRequest;
import org.example.jaipark_back.dto.CommentResponse;
import org.example.jaipark_back.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@CrossOrigin(origins = "http://localhost:3000")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(commentService.createComment(postId, request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyComments(Authentication authentication) {
        return ResponseEntity.ok(commentService.getCommentsByUsername(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long postId,
            @PathVariable Long id,
            @RequestBody CommentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(commentService.updateComment(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long id,
            Authentication authentication) {
        commentService.deleteComment(id, authentication.getName());
        return ResponseEntity.ok().build();
    }
} 