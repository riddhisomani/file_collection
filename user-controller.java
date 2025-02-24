import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

/**
 * REST controller for managing user-related operations
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        UserDto createdUser = userService.registerUser(registrationDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Update user profile
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateDto userUpdateDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Check if the user is updating their own profile
        UserDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        if (!currentUser.getUserId().equals(userId) && !currentUser.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        UserDto updatedUser = userService.updateUserProfile(userId, userUpdateDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Toggle profile privacy
     */
    @PutMapping("/{userId}/privacy")
    public ResponseEntity<UserDto> togglePrivacy(
            @PathVariable String userId,
            @RequestParam boolean isPrivate,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Check if the user is updating their own profile
        UserDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        if (!currentUser.getUserId().equals(userId) && !currentUser.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        UserDto updatedUser = userService.toggleProfilePrivacy(userId, isPrivate);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Follow a user
     */
    @PostMapping("/{userId}/follow")
    public ResponseEntity<Void> followUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UserDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        boolean success = userService.followUser(currentUser.getUserId(), userId);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Unfollow a user
     */
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UserDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        boolean success = userService.unfollowUser(currentUser.getUserId(), userId);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user's followers
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<UserDto>> getUserFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<UserDto> followers = userService.getUserFollowers(userId, page, size);
        return ResponseEntity.ok(followers);
    }

    /**
     * Get users followed by a user
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<UserDto>> getUserFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<UserDto> following = userService.getUserFollowing(userId, page, size);
        return ResponseEntity.ok(following);
    }

    /**
     * Create an admin user (admin only)
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createAdmin(
            @Valid @RequestBody UserRegistrationDto adminDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UserDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        UserDto createdAdmin = userService.createAdmin(adminDto, currentUser.getUserId());
        return new ResponseEntity<>(createdAdmin, HttpStatus.CREATED);
    }

    /**
     * Bulk import users (admin only)
     */
    @PostMapping("/bulk-import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportResultDto> bulkImportUsers(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UserDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        int importCount = userService.bulkImportUsers(file, currentUser.getUserId());
        
        ImportResultDto result = new ImportResultDto();
        result.setSuccessCount(importCount);
        result.setMessage(importCount + " users successfully imported");
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get users ordered by follower count (admin only)
     */
    @GetMapping("/stats/followers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserStatsDto>> getUsersByFollowerCount(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<UserStatsDto> userStats = userService.getUsersByFollowerCount(page, size);
        return ResponseEntity.ok(userStats);
    }
}

// Simple DTO for bulk import results
class ImportResultDto {
    private int successCount;
    private String message;
    
    // Getters and setters
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
