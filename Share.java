//Share Controller file
package com.socio.controller;

import com.socio.entity.Share;
import com.socio.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shares")
public class ShareController {

    @Autowired
    private ShareService shareService;

    @PostMapping("/sharePost")
    public ResponseEntity<Share> sharePostWithUser(@RequestParam Long postId, 
                                                   @RequestParam Long senderId, 
                                                   @RequestParam Long receiverId) {
        Share share = shareService.sharePostWithUser(postId, senderId, receiverId);
        return ResponseEntity.ok(share);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Share>> getSharesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(shareService.getSharesByUser(userId));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Share>> getSharesByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(shareService.getSharesByPost(postId));
    }
}

//ShareService file:
package com.socio.service;

import com.socio.entity.Post;
import com.socio.entity.Share;
import com.socio.entity.User;
import com.socio.repository.PostRepository;
import com.socio.repository.ShareRepository;
import com.socio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShareService {

    @Autowired
    private ShareRepository shareRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    public Share sharePostWithUser(Long postId, Long senderId, Long receiverId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Share share = new Share();
        share.setPost(post);
        share.setSender(sender);
        share.setReceiver(receiver);
        return shareRepository.save(share);
    }

    public List<Share> getSharesByUser(Long userId) {
        return shareRepository.findByReceiverUserId(userId);
    }

    public List<Share> getSharesByPost(Long postId) {
        return shareRepository.findByPostPostId(postId);
    }
}

//ReportService file
package com.socio.service;

import com.socio.entity.Post;
import com.socio.entity.Report;
import com.socio.entity.User;
import com.socio.repository.PostRepository;
import com.socio.repository.ReportRepository;
import com.socio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    public Report reportPost(Long postId, Long reporterId, String reason) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        Report report = new Report();
        report.setPost(post);
        report.setReporter(reporter);
        report.setReason(reason);
        return reportRepository.save(report);
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public void deleteReport(Long reportId) {
        reportRepository.deleteById(reportId);
    }
}

//ReportController 
package com.socio.controller;

import com.socio.entity.Report;
import com.socio.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/reportPost")
    public ResponseEntity<Report> reportPost(@RequestParam Long postId, 
                                             @RequestParam Long reporterId, 
                                             @RequestParam String reason) {
        Report report = reportService.reportPost(postId, reporterId, reason);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<String> deleteReport(@PathVariable Long reportId) {
        reportService.deleteReport(reportId);
        return ResponseEntity.ok("Report deleted successfully");
    }
}

