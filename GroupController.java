package com.socio.controller;

import com.socio.entity.Group;
import com.socio.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping("/create")
    public ResponseEntity<Group> createGroup(@RequestParam Long creatorId, 
                                             @RequestParam String groupName, 
                                             @RequestParam boolean isPrivate) {
        Group group = groupService.createGroup(creatorId, groupName, isPrivate);
        return ResponseEntity.ok(group);
    }

    @PutMapping("/{groupId}/togglePrivacy")
    public ResponseEntity<String> toggleGroupPrivacy(@PathVariable Long groupId, 
                                                     @RequestParam Long userId) {
        groupService.toggleGroupPrivacy(groupId, userId);
        return ResponseEntity.ok("Group privacy updated");
    }

    @PostMapping("/{groupId}/addMember")
    public ResponseEntity<String> addMember(@PathVariable Long groupId, 
                                            @RequestParam Long userId, 
                                            @RequestParam Long creatorId) {
        groupService.addMember(groupId, userId, creatorId);
        return ResponseEntity.ok("User added to group");
    }

    @DeleteMapping("/{groupId}/removeMember")
    public ResponseEntity<String> removeMember(@PathVariable Long groupId, 
                                               @RequestParam Long userId, 
                                               @RequestParam Long creatorId) {
        groupService.removeMember(groupId, userId, creatorId);
        return ResponseEntity.ok("User removed from group");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Group>> getUserGroups(@PathVariable Long userId) {
        return ResponseEntity.ok(groupService.getUserGroups(userId));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<Optional<Group>> getGroupDetails(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupDetails(groupId));
    }
}
