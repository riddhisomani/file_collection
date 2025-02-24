package com.socio.controller;

import com.socio.dto.PostDto;
import com.socio.dto.PostRequest;
import com.socio.security.CurrentUser;
import com.socio.security.UserPrincipal;
import com.socio.service.PostService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for managing posts
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Api(tags = "Post Management")
public class PostController {

    private final PostService postService;

    /**
     * Create a new post
     */
    @PostMapping
    @ApiOperation("Create a new post")
    public ResponseEntity<PostDto> createPost(
            @CurrentUser UserPrincipal currentUser,
            @RequestPart("request") PostRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        
        PostDto createdPost = postService.createPost(currentUser.getId(), request, file);
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    /**
     * Get a post by ID
     */
    @GetMapping("/{postId}")
    @ApiOperation("Get a post by ID")
    public ResponseEntity<PostDto> getPostById(
            @PathVariable Long postId,
            @CurrentUser UserPrincipal currentUser) {
        
        PostDto post = postService.getPostById(postId, currentUser.getId());
        return ResponseEntity.ok(post);
    }

    /**
     * Delete a post
     */
    @DeleteMapping("/{postId}")
    @ApiOperation("Delete a post")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @CurrentUser UserPrincipal currentUser) {
        
        boolean deleted = postService.deletePost(postId, currentUser.getId());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Get posts by user ID
     */
    @GetMapping("/user/{userId}")
    @ApiOperation("Get posts by user ID")
    public ResponseEntity<List<PostDto>> getPostsByUserId(
            @PathVariable Long userId,
            @CurrentUser UserPrincipal currentUser) {
        
        List<PostDto> posts = postService.getPostsByUserId(userId, currentUser.getId());
        return ResponseEntity.ok(posts);
    }

    /**
     * Get feed for current user
     */
    @GetMapping("/feed")
    @ApiOperation("Get feed for current user")
    public ResponseEntity<Page<PostDto>> getFeed(
            @CurrentUser UserPrincipal currentUser,
            Pageable pageable) {
        
        Page<PostDto> feed = postService.getFeedForUser(currentUser.getId(), pageable);
        return ResponseEntity.ok(feed);
    }

    /**
     * Share a post
     */
    @PostMapping("/{postId}/share")
    @ApiOperation("Share a post")
    public ResponseEntity<PostDto> sharePost(
            @PathVariable Long postId,
            @RequestParam(required = false) String content,
            @CurrentUser UserPrincipal currentUser) {
        
        PostDto sharedPost = postService.sharePost(currentUser.getId(), postId, content);
        return new ResponseEntity<>(sharedPost, HttpStatus.CREATED);
    }

    /**
     * Get posts by group ID
     */
    @GetMapping("/group/{groupId}")
    @ApiOperation("Get posts by group ID")
    public ResponseEntity<List<PostDto>> getPostsByGroupId(
            @PathVariable Long groupId,
            @CurrentUser UserPrincipal currentUser) {
        
        List<PostDto> posts = postService.getPostsByGroupId(groupId, currentUser.getId());
        return ResponseEntity.ok(posts);
    }

    /**
     * Create a post in a group
     */
    @PostMapping("/group/{groupId}")
    @ApiOperation("Create a post in a group")
    public ResponseEntity<PostDto> createGroupPost(
            @PathVariable Long groupId,
            @CurrentUser UserPrincipal currentUser,
            @RequestPart("request") PostRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        
        PostDto createdPost = postService.createGroupPost(currentUser.getId(), groupId, request, file);
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    /**
     * Get posts by engagement (admin only)
     */
    @GetMapping("/engagement")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiOperation("Get posts by engagement (admin only)")
    public ResponseEntity<Page<PostDto>> getPostsByEngagement(Pageable pageable) {
        Page<PostDto> posts = postService.getPostsByEngagement(pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get posts by file type (admin only)
     */
    @GetMapping("/fileType/{fileType}")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiOperation("Get posts by file type (admin only)")
    public ResponseEntity<Page<PostDto>> getPostsByFileType(
            @PathVariable String fileType,
            Pageable pageable) {
        
        Page<PostDto> posts = postService.getPostsByFileType(fileType, pageable);
        return ResponseEntity.ok(posts);
    }
}
