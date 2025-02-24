// LikeService.java
@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public void likePost(Long postId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        if (likeRepository.existsByUserAndPost(user, post)) {
            throw new IllegalStateException("Post already liked");
        }

        likeRepository.save(new Like(user, post));
        post.incrementLikeCount();
    }

    public void unlikePost(Long postId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        Like like = likeRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new EntityNotFoundException("Like not found"));

        likeRepository.delete(like);
        post.decrementLikeCount();
    }
}

// LikeController.java
@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/{postId}")
    public ResponseEntity<Void> likePost(@PathVariable Long postId, @AuthenticationPrincipal String email) {
        likeService.likePost(postId, email);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId, @AuthenticationPrincipal String email) {
        likeService.unlikePost(postId, email);
        return ResponseEntity.noContent().build();
    }
}
