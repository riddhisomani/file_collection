package com.socio.service.impl;

import com.socio.dto.PostDto;
import com.socio.dto.PostRequest;
import com.socio.entity.*;
import com.socio.exception.ForbiddenException;
import com.socio.exception.ResourceNotFoundException;
import com.socio.repository.*;
import com.socio.service.FileService;
import com.socio.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the PostService interface
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final FileService fileService;

    /**
     * Create a new post
     */
    @Override
    @Transactional
    public PostDto createPost(Long userId, PostRequest request, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Post post = new Post();
        post.setContent(request.getContent());
        post.setUserId(userId);
        post.setCreatedAt(LocalDateTime.now());

        // Handle file upload if present
        if (file != null && !file.isEmpty()) {
            String fileUrl = fileService.uploadFile(file);
            post.setFileUrl(fileUrl);
            post.setFileType(getFileType(file.getContentType()));
        }

        // Handle shared post
        if (request.getOriginalPostId() != null) {
            Post originalPost = postRepository.findById(request.getOriginalPostId())
                    .orElseThrow(() -> new ResourceNotFoundException("Original post not found"));
            
            post.setIsShared(true);
            post.setOriginalPostId(originalPost.getId());
            post.setOriginalUserId(originalPost.getUserId());
        }

        Post savedPost = postRepository.save(post);
        return convertToDto(savedPost, userId);
    }

    /**
     * Get a post by its ID
     */
    @Override
    @Cacheable(value = "posts", key = "#postId")
    public PostDto getPostById(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        User postOwner = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Post owner not found"));
                
        // Check if current user can view this post
        if (postOwner.isPrivate() && !postOwner.getId().equals(currentUserId)) {
            // Check if current user follows the post owner
            boolean isFollowing = followRepository.findByFollowerIdAndFolloweeId(currentUserId, post.getUserId()).isPresent();
            
            // If not following and not an admin, forbid access
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
            
            if (!isFollowing && !currentUser.isAdmin()) {
                throw new ForbiddenException("Cannot view post from private profile");
            }
        }
        
        return convertToDto(post, currentUserId);
    }

    /**
     * Delete a post
     */
    @Override
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public boolean deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Only post owner or admin can delete a post
        if (!post.getUserId().equals(userId) && !user.isAdmin()) {
            throw new ForbiddenException("Not authorized to delete this post");
        }
        
        // Delete file if exists
        if (post.getFileUrl() != null) {
            fileService.deleteFile(post.getFileUrl());
        }
        
        postRepository.delete(post);
        return true;
    }

    /**
     * Get all posts created by a specific user
     */
    @Override
    @Cacheable(value = "userPosts", key = "#userId")
    public List<PostDto> getPostsByUserId(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check if current user can view posts from this user
        if (user.isPrivate() && !userId.equals(currentUserId)) {
            // Check if current user follows the requested user
            boolean isFollowing = followRepository.findByFollowerIdAndFolloweeId(currentUserId, userId).isPresent();
            
            // If not following and not an admin, forbid access
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
            
            if (!isFollowing && !currentUser.isAdmin()) {
                throw new ForbiddenException("Cannot view posts from private profile");
            }
        }
        
        List<Post> posts = postRepository.findByUserId(userId);
        return posts.stream()
                .map(post -> convertToDto(post, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Get the feed for a user (posts from followed users and own posts)
     */
    @Override
    @Cacheable(value = "userFeed", key = "#userId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PostDto> getFeedForUser(Long userId, Pageable pageable) {
        // Get posts from user and followed users
        Page<Post> posts = postRepository.findFeedForUser(userId, pageable);
        
        return posts.map(post -> convertToDto(post, userId));
    }

    /**
     * Share an existing post
     */
    @Override
    @Transactional
    public PostDto sharePost(Long userId, Long originalPostId, String content) {
        // Verify original post exists
        Post originalPost = postRepository.findById(originalPostId)
                .orElseThrow(() -> new ResourceNotFoundException("Original post not found"));
        
        // Verify current user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check if original post owner is private
        User originalPostOwner = userRepository.findById(originalPost.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Original post owner not found"));
        
        if (originalPostOwner.isPrivate()) {
            // Check if current user follows the original post owner
            boolean isFollowing = followRepository.findByFollowerIdAndFolloweeId(userId, originalPost.getUserId()).isPresent();
            
            // If not following and not an admin, forbid sharing
            User currentUser = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
            
            if (!isFollowing && !currentUser.isAdmin() && !userId.equals(originalPost.getUserId())) {
                throw new ForbiddenException("Cannot share post from private profile");
            }
        }
        
        // Create shared post
        Post sharedPost = new Post();
        sharedPost.setContent(content);
        sharedPost.setUserId(userId);
        sharedPost.setCreatedAt(LocalDateTime.now());
        sharedPost.setIsShared(true);
        sharedPost.setOriginalPostId(originalPostId);
        sharedPost.setOriginalUserId(originalPost.getUserId());
        
        Post savedPost = postRepository.save(sharedPost);
        return convertToDto(savedPost, userId);
    }

    /**
     * Get all posts for a specific group
     */
    @Override
    @Cacheable(value = "groupPosts", key = "#groupId")
    public List<PostDto> getPostsByGroupId(Long groupId, Long currentUserId) {
        // Verify group exists
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        // Check if current user is member of the group or an admin
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        
        if (group.isPrivate() && !group.getCreatorId().equals(currentUserId)) {
            boolean isMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId).isPresent();
            
            if (!isMember && !currentUser.isAdmin()) {
                throw new ForbiddenException("Cannot view posts from private group");
            }
        }
        
        List<Post> posts = postRepository.findByGroupId(groupId);
        return posts.stream()
                .map(post -> convertToDto(post, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Create a post in a group
     */
    @Override
    @Transactional
    public PostDto createGroupPost(Long userId, Long groupId, PostRequest request, MultipartFile file) {
        // Verify group exists
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        // Check if current user is member of the group
        boolean isMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent() || 
                           group.getCreatorId().equals(userId);
        
        if (!isMember) {
            throw new ForbiddenException("Only group members can post in a group");
        }
        
        // Create post (reuse existing logic)
        return createPost(userId, request, file);
    }

    /**
     * Create a birthday post for a user
     */
    @Override
    @Transactional
    public Post createBirthdayPost(Long userId) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Create birthday post
        Post birthdayPost = new Post();
        birthdayPost.setContent("Happy Birthday, " + user.getName() + "! 🎂 From SOCIO");
        birthdayPost.setUserId(userId); // Post appears on user's own wall
        birthdayPost.setCreatedAt(LocalDateTime.now());
        
        return postRepository.save(birthdayPost);
    }

    /**
     * Get posts ordered by engagement (likes and comments)
     */
    @Override
    @Cacheable(value = "postsEngagement", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PostDto> getPostsByEngagement(Pageable pageable) {
        // This would typically involve a custom query with join and order by
        // For simplicity, we're fetching all posts and then sorting
        Page<Post> posts = postRepository.findAll(pageable);
        
        return posts.map(post -> {
            PostDto dto = convertToDto(post, null);
            // Sort is handled by the pageable parameter
            return dto;
        });
    }

    /**
     * Get posts by file type
     */
    @Override
    @Cacheable(value = "postsByFileType", key = "#fileType + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PostDto> getPostsByFileType(String fileType, Pageable pageable) {
        // This would typically be a custom query
        // For simplicity, we're filtering after fetching
        Page<Post> allPosts = postRepository.findAll(pageable);
        
        return allPosts
                .filter(post -> fileType.equals(post.getFileType()))
                .map(post -> convertToDto(post, null));
    }

    /**
     * Helper method to convert Post entity to PostDto
     */
    private PostDto convertToDto(Post post, Long currentUserId) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setFileUrl(post.getFileUrl());
        dto.setFileType(post.getFileType());
        dto.setUserId(post.getUserId());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setShared(post.isShared());
        dto.setOriginalPostId(post.getOriginalPostId());
        dto.setOriginalUserId(post.getOriginalUserId());
        
        // Get user name
        userRepository.findById(post.getUserId()).ifPresent(user -> dto.setUserName(user.getName()));
        
        // Get original user name if shared
        if (post.isShared() && post.getOriginalUserId() != null) {
            userRepository.findById(post.getOriginalUserId()).ifPresent(user -> dto.setOriginalUserName(user.getName()));
        }
        
        // Get like count
        long likeCount = likeRepository.countByPostId(post.getId());
        dto.setLikeCount(likeCount);
        
        // Get comment count
        long commentCount = commentRepository.countByPostId(post.getId());
        dto.setCommentCount(commentCount);
        
        // Check if current user liked this post
        if (currentUserId != null) {
            boolean likedByCurrentUser = likeRepository.findByPostIdAndUserId(post.getId(), currentUserId).isPresent();
            dto.setLikedByCurrentUser(likedByCurrentUser);
        }
        
        return dto;
    }

    /**
     * Helper method to determine file type from content type
     */
    private String getFileType(String contentType) {
        if (contentType == null) {
            return null;
        }
        
        if (contentType.startsWith("image/")) {
            return "IMAGE";
        } else if (contentType.startsWith("video/")) {
            return "VIDEO";
        } else if (contentType.equals("application/pdf")) {
            return "PDF";
        } else if (contentType.startsWith("audio/")) {
            return "AUDIO";
        } else {
            return "DOCUMENT";
        }
    }
}
