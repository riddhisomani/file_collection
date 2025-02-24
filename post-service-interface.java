package com.socio.service;

import com.socio.dto.PostDto;
import com.socio.dto.PostRequest;
import com.socio.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for managing posts in the SOCIO platform
 */
public interface PostService {
    
    /**
     * Create a new post
     * 
     * @param userId the ID of the user creating the post
     * @param request the post request containing content and other details
     * @param file optional file attachment
     * @return the created post as a DTO
     */
    PostDto createPost(Long userId, PostRequest request, MultipartFile file);
    
    /**
     * Get a post by its ID
     * 
     * @param postId the ID of the post to retrieve
     * @param currentUserId the ID of the current user
     * @return the post as a DTO
     */
    PostDto getPostById(Long postId, Long currentUserId);
    
    /**
     * Delete a post
     * 
     * @param postId the ID of the post to delete
     * @param userId the ID of the user requesting deletion
     * @return true if deleted successfully
     */
    boolean deletePost(Long postId, Long userId);
    
    /**
     * Get all posts created by a specific user
     * 
     * @param userId the ID of the user whose posts to retrieve
     * @param currentUserId the ID of the current user
     * @return list of posts as DTOs
     */
    List<PostDto> getPostsByUserId(Long userId, Long currentUserId);
    
    /**
     * Get the feed for a user (posts from followed users and own posts)
     * 
     * @param userId the ID of the user requesting the feed
     * @param pageable pagination information
     * @return page of posts as DTOs
     */
    Page<PostDto> getFeedForUser(Long userId, Pageable pageable);
    
    /**
     * Share an existing post
     * 
     * @param userId the ID of the user sharing the post
     * @param originalPostId the ID of the post being shared
     * @param content optional additional content for the share
     * @return the new shared post as a DTO
     */
    PostDto sharePost(Long userId, Long originalPostId, String content);
    
    /**
     * Get all posts for a specific group
     * 
     * @param groupId the ID of the group
     * @param currentUserId the ID of the current user
     * @return list of posts as DTOs
     */
    List<PostDto> getPostsByGroupId(Long groupId, Long currentUserId);
    
    /**
     * Create a post in a group
     * 
     * @param userId the ID of the user creating the post
     * @param groupId the ID of the group
     * @param request the post request
     * @param file optional file attachment
     * @return the created post as a DTO
     */
    PostDto createGroupPost(Long userId, Long groupId, PostRequest request, MultipartFile file);
    
    /**
     * Create a birthday post for a user
     * 
     * @param userId the ID of the user having a birthday
     * @return the created birthday post
     */
    Post createBirthdayPost(Long userId);
    
    /**
     * Get posts ordered by engagement (likes and comments)
     * 
     * @param pageable pagination information
     * @return page of posts ordered by engagement
     */
    Page<PostDto> getPostsByEngagement(Pageable pageable);
    
    /**
     * Get posts by file type
     * 
     * @param fileType the file type to filter by
     * @param pageable pagination information
     * @return page of posts with the specified file type
     */
    Page<PostDto> getPostsByFileType(String fileType, Pageable pageable);
}
