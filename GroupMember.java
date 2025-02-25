package com.socio.service;

import com.socio.entity.Group;
import com.socio.entity.GroupMember;
import com.socio.entity.User;
import com.socio.repository.GroupMemberRepository;
import com.socio.repository.GroupRepository;
import com.socio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GroupMemberService {

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupGroupId(groupId);
    }
}
