// CommentService.java
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public Comment addComment(Long postId, String content, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        Comment comment = new Comment(user, post, content);
        post.incrementCommentCount();
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        if (!comment.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("Not authorized to delete this comment");
        }
        commentRepository.delete(comment);
        comment.getPost().decrementCommentCount();
    }

    public List<Comment> getCommentsByPost(Long postId) {
        return commentRepository.findByPostId(postId);
    }
}

// CommentController.java
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/{postId}")
    public ResponseEntity<Comment> addComment(@PathVariable Long postId, @RequestBody CommentRequest request, @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(commentService.addComment(postId, request.getContent(), email));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, @AuthenticationPrincipal String email) {
        commentService.deleteComment(commentId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }
}
