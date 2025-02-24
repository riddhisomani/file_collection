// FollowService.java
@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public void followUser(Long followingId, String email) {
        User follower = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new IllegalStateException("Already following this user");
        }

        followRepository.save(new Follow(follower, following));
    }

    public void unfollowUser(Long followingId, String email) {
        User follower = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Follow follow = followRepository.findByFollowerAndFollowing(follower, following)
                .orElseThrow(() -> new EntityNotFoundException("Follow relation not found"));

        followRepository.delete(follow);
    }
}

// FollowController.java
@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{userId}")
    public ResponseEntity<Void> followUser(@PathVariable Long userId, @AuthenticationPrincipal String email) {
        followService.followUser(userId, email);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfollowUser(@PathVariable Long userId, @AuthenticationPrincipal String email) {
        followService.unfollowUser(userId, email);
        return ResponseEntity.noContent().build();
    }
}
