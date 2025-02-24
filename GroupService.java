package com.socio.service;

import com.socio.entity.Group;
import com.socio.entity.GroupMember;
import com.socio.entity.User;
import com.socio.repository.GroupMemberRepository;
import com.socio.repository.GroupRepository;
import com.socio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    public Group createGroup(Long creatorId, String groupName, boolean isPrivate) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Group group = new Group();
        group.setName(groupName);
        group.setCreator(creator);
        group.setPrivate(isPrivate);
        groupRepository.save(group);

        // Add creator as the first member
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(creator);
        groupMemberRepository.save(groupMember);

        return group;
    }

    public void toggleGroupPrivacy(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new RuntimeException("Only the group creator can change privacy settings");
        }

        group.setPrivate(!group.isPrivate());
        groupRepository.save(group);
    }

    public void addMember(Long groupId, Long userId, Long creatorId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getCreator().getUserId().equals(creatorId)) {
            throw new RuntimeException("Only the creator can add members");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMemberRepository.save(groupMember);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId, Long creatorId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getCreator().getUserId().equals(creatorId)) {
            throw new RuntimeException("Only the creator can remove members");
        }

        groupMemberRepository.deleteByGroupGroupIdAndUserUserId(groupId, userId);
    }

    public List<Group> getUserGroups(Long userId) {
        return groupRepository.findByMembersUserUserId(userId);
    }

    public Optional<Group> getGroupDetails(Long groupId) {
        return groupRepository.findById(groupId);
    }
}
