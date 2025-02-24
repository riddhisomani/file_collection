// UserService.java
import java.util.List;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends UserDetailsService {
    /**
     * Register a new user with email and password
     * 
     * @param userDto The registration details
     * @return The registered user
     */
    UserDto registerUser(UserRegistrationDto userDto);
    
    /**
     * Update the user profile
     * 
     * @param userId The user ID
     * @param userDto The updated user details
     * @return The updated user
     */
    UserDto updateUserProfile(String userId, UserUpdateDto userDto);
    
    /**
     * Toggle user profile privacy setting
     * 
     * @param userId The user ID
     * @param isPrivate The privacy setting
     * @return The updated user
     */
    UserDto toggleProfilePrivacy(String userId, boolean isPrivate);
    
    /**
     * Create a new admin user (only admins can create other admins)
     * 
     * @param adminDto The admin registration details
     * @param creatorId The ID of the admin creating this new admin
     * @return The created admin user
     */
    UserDto createAdmin(UserRegistrationDto adminDto, String creatorId);
    
    /**
     * Import multiple users from a CSV or XLSX file
     * 
     * @param file The file containing user data
     * @param adminId The ID of the admin importing the users
     * @return Number of users successfully imported
     */
    int bulkImportUsers(MultipartFile file, String adminId);
    
    /**
     * Follow another user
     * 
     * @param followerId The ID of the user doing the following
     * @param followingId The ID of the user to follow
     * @return true if successful
     */
    boolean followUser(String followerId, String followingId);
    
    /**
     * Unfollow a user
     * 
     * @param followerId The ID of the user doing the unfollowing
     * @param followingId The ID of the user to unfollow
     * @return true if successful
     */
    boolean unfollowUser(String followerId, String followingId);
    
    /**
     * Get user by ID
     * 
     * @param userId The user ID
     * @return The user
     */
    UserDto getUserById(String userId);
    
    /**
     * Get all users ordered by follower count grouped by dates
     * 
     * @param page The page number
     * @param size The page size
     * @return List of users with statistics
     */
    List<UserStatsDto> getUsersByFollowerCount(int page, int size);
    
    /**
     * Check if a user is an admin
     * 
     * @param userId The user ID
     * @return true if the user is an admin
     */
    boolean isAdmin(String userId);
    
    /**
     * Get a user's followers
     * 
     * @param userId The user ID
     * @param page The page number
     * @param size The page size
     * @return List of followers
     */
    List<UserDto> getUserFollowers(String userId, int page, int size);
    
    /**
     * Get users a user is following
     * 
     * @param userId The user ID
     * @param page The page number
     * @param size The page size
     * @return List of users being followed
     */
    List<UserDto> getUserFollowing(String userId, int page, int size);
}

// UserServiceImpl.java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserServiceImpl(UserRepository userRepository, 
                          FollowRepository followRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.isAdmin() ? "ROLE_ADMIN" : "ROLE_USER"));
        
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), 
            user.getPassword(), 
            authorities
        );
    }
    
    @Override
    @Transactional
    public UserDto registerUser(UserRegistrationDto userDto) {
        // Check if email already exists
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setName(userDto.getName());
        user.setDateOfBirth(userDto.getDateOfBirth());
        user.setAdmin(false); // Regular users are not admins by default
        
        User savedUser = userRepository.save(user);
        return mapUserToDto(savedUser);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public UserDto updateUserProfile(String userId, UserUpdateDto userDto) {
        User user = getUserEntityById(userId);
        
        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        
        if (userDto.getDateOfBirth() != null) {
            user.setDateOfBirth(userDto.getDateOfBirth());
        }
        
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public UserDto toggleProfilePrivacy(String userId, boolean isPrivate) {
        User user = getUserEntityById(userId);
        user.setPrivate(isPrivate);
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }
    
    @Override
    @Transactional
    public UserDto createAdmin(UserRegistrationDto adminDto, String creatorId) {
        // Verify creator is an admin
        User creator = getUserEntityById(creatorId);
        if (!creator.isAdmin()) {
            throw new UnauthorizedAccessException("Only admins can create other admins");
        }
        
        // Validate admin email domain
        if (!adminDto.getEmail().endsWith("@socio.com")) {
            throw new InvalidAdminEmailException("Admin email must end with @socio.com");
        }
        
        // Create new admin user
        User admin = new User();
        admin.setEmail(adminDto.getEmail());
        admin.setPassword(passwordEncoder.encode(adminDto.getPassword()));
        admin.setName(adminDto.getName());
        admin.setDateOfBirth(adminDto.getDateOfBirth());
        admin.setAdmin(true);
        
        User savedAdmin = userRepository.save(admin);
        return mapUserToDto(savedAdmin);
    }
    
    @Override
    @Transactional
    public int bulkImportUsers(MultipartFile file, String adminId) {
        // Verify user is an admin
        User admin = getUserEntityById(adminId);
        if (!admin.isAdmin()) {
            throw new UnauthorizedAccessException("Only admins can import users");
        }
        
        int importCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            // Skip header
            reader.readLine();
            
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 3) { // Minimum: email, password, name
                    try {
                        // Create user if email doesn't exist
                        if (!userRepository.existsByEmail(data[0])) {
                            User user = new User();
                            user.setEmail(data[0]);
                            user.setPassword(passwordEncoder.encode(data[1]));
                            user.setName(data[2]);
                            // Set additional fields if available
                            
                            userRepository.save(user);
                            importCount++;
                        }
                    } catch (Exception e) {
                        // Log error but continue processing
                        System.err.println("Error importing user: " + data[0] + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw new FileProcessingException("Error processing import file: " + e.getMessage());
        }
        
        return importCount;
    }
    
    @Override
    @Transactional
    public boolean followUser(String followerId, String followingId) {
        // Cannot follow yourself
        if (followerId.equals(followingId)) {
            return false;
        }
        
        // Check if both users exist
        User follower = getUserEntityById(followerId);
        User following = getUserEntityById(followingId);
        
        // Check if already following
        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            return false;
        }
        
        // Create follow relationship
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        
        followRepository.save(follow);
        return true;
    }
    
    @Override
    @Transactional
    public boolean unfollowUser(String followerId, String followingId) {
        User follower = getUserEntityById(followerId);
        User following = getUserEntityById(followingId);
        
        Optional<Follow> follow = followRepository.findByFollowerAndFollowing(follower, following);
        if (follow.isPresent()) {
            followRepository.delete(follow.get());
            return true;
        }
        
        return false;
    }
    
    @Override
    @Cacheable(value = "userProfile", key = "#userId")
    public UserDto getUserById(String userId) {
        User user = getUserEntityById(userId);
        return mapUserToDto(user);
    }
    
    @Override
    public List<UserStatsDto> getUsersByFollowerCount(int page, int size) {
        return userRepository.findAllOrderedByFollowerCount(PageRequest.of(page, size))
                .stream()
                .map(this::mapUserToStatsDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isAdmin(String userId) {
        User user = getUserEntityById(userId);
        return user.isAdmin();
    }
    
    @Override
    public List<UserDto> getUserFollowers(String userId, int page, int size) {
        User user = getUserEntityById(userId);
        
        return followRepository.findFollowersByFollowing(user, PageRequest.of(page, size))
                .stream()
                .map(Follow::getFollower)
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UserDto> getUserFollowing(String userId, int page, int size) {
        User user = getUserEntityById(userId);
        
        return followRepository.findFollowingsByFollower(user, PageRequest.of(page, size))
                .stream()
                .map(Follow::getFollowing)
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private User getUserEntityById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }
    
    private UserDto mapUserToDto(User user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setPrivate(user.isPrivate());
        dto.setAdmin(user.isAdmin());
        dto.setCreatedAt(user.getCreatedAt());
        
        // Optionally include follower count
        Long followerCount = followRepository.countFollowersByFollowing(user);
        dto.setFollowerCount(followerCount != null ? followerCount : 0);
        
        return dto;
    }
    
    private UserStatsDto mapUserToStatsDto(User user) {
        UserStatsDto dto = new UserStatsDto();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        
        // Get follower count
        Long followerCount = followRepository.countFollowersByFollowing(user);
        dto.setFollowerCount(followerCount != null ? followerCount : 0);
        
        // Registration date grouping
        dto.setRegistrationDate(user.getCreatedAt().toLocalDate());
        
        return dto;
    }
}
