package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.dto.VaultGroupDTO;
import me.aloic.apeurival.entity.dto.VaultGroupRequest;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.VaultGroupMapper;
import me.aloic.apeurival.entity.mapper.VaultGroupMemberMapper;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.VaultGroupMemberPO;
import me.aloic.apeurival.entity.po.VaultGroupPO;
import me.aloic.apeurival.service.VaultGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VaultGroupServiceImpl implements VaultGroupService {

    private final VaultGroupMapper groupMapper;
    private final VaultGroupMemberMapper memberMapper;
    private final UserMapper userMapper;

    public VaultGroupServiceImpl(VaultGroupMapper groupMapper,
                                  VaultGroupMemberMapper memberMapper,
                                  UserMapper userMapper) {
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
    }

    @Override
    public VaultGroupDTO createNewVaultGroup(VaultGroupRequest req, Long userId, String userRole) {
        if (!"ADMIN".equals(userRole)) {
            log.warn("Permission denied for {} to create {} group",userId,req.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can create groups");
        }
        VaultGroupPO po = new VaultGroupPO();
        po.setName(req.getName());
        po.setDescription(req.getDescription());
        po.setCreatedAt(LocalDateTime.now());
        groupMapper.insert(po);
        log.info("Created new group: {}",req.getName());
        return setupVaultGroup(po);
    }

    @Override
    public List<VaultGroupDTO> listAllVaultGroup(Long currentUserId, String userRole) {
        boolean isAdmin = "ADMIN".equals(userRole);
        return groupMapper.selectList(new QueryWrapper<>()).stream()
                .map(po -> setupVaultGroup(po, currentUserId, isAdmin))
                .toList();
    }

    @Override
    public VaultGroupDTO update(Long id, VaultGroupRequest req, Long userId, String userRole) {
        checkGroupManager(id, userId, userRole);
        VaultGroupPO po = groupMapper.selectById(id);
        if (po == null)
        {
            log.warn("Cant found such group:{}",id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
        if (req.getName() != null) po.setName(req.getName());
        if (req.getDescription() != null) po.setDescription(req.getDescription());
        groupMapper.updateById(po);
        log.info("Updated group: {}",id);
        return setupVaultGroup(po);
    }

    @Override
    public void delete(Long id, Long userId, String userRole) {
        checkGroupManager(id, userId, userRole);
        memberMapper.delete(new QueryWrapper<VaultGroupMemberPO>().eq("group_id", id));
        groupMapper.deleteById(id);
        log.info("Deleted group: {}", id);
    }

    @Override
    public void addMember(Long groupId, Long userId, String role, Long callerId, String callerRole) {
        checkGroupManager(groupId, callerId, callerRole);
        if (!groupMapper.exists(new QueryWrapper<VaultGroupPO>().eq("id", groupId)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        if (!userMapper.exists(new QueryWrapper<UserPO>().eq("id", userId)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

        VaultGroupMemberPO member = new VaultGroupMemberPO();
        member.setGroupId(groupId);
        member.setUserId(userId);
        String targetRole = "MEMBER";
        if (role != null && callerRole!=null && callerRole.equalsIgnoreCase("ADMIN")) {
            targetRole = role.equalsIgnoreCase("MANAGER") ? "MANAGER" : "MEMBER";
        }
        member.setRole(targetRole);
        memberMapper.insert(member);
        log.info("Added user {} to group {} as {}", userId, groupId, member.getRole());
    }

    @Override
    public void removeMember(Long groupId, Long userId, Long callerId, String callerRole) {
        checkGroupManager(groupId, callerId, callerRole);
        memberMapper.delete(new QueryWrapper<VaultGroupMemberPO>()
                .eq("group_id", groupId).eq("user_id", userId));
        log.info("Removed user {} from group {}", userId, groupId);
    }

    private void checkGroupManager(Long groupId, Long userId, String userRole) {
        if ("ADMIN".equals(userRole))
        {
            log.info("user role is ADMIN, userid: {}",userId);
            return;
        }
        VaultGroupMemberPO self = memberMapper.selectOne(
                new QueryWrapper<VaultGroupMemberPO>()
                        .eq("group_id", groupId).eq("user_id", userId));
        if (self != null && "MANAGER".equals(self.getRole()))
        {
            log.info("user role is MANAGER, userid: {}",userId);
            return;
        }
        log.warn("user role not met, userid: {}",userId);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group managers or admins can manage groups");
    }

    private VaultGroupDTO setupVaultGroup(VaultGroupPO po) {
        VaultGroupDTO dto = new VaultGroupDTO();
        dto.setId(po.getId());
        dto.setName(po.getName());
        dto.setDescription(po.getDescription());
        dto.setCreatedAt(po.getCreatedAt());

        List<VaultGroupMemberPO> members = memberMapper.selectList(
                new QueryWrapper<VaultGroupMemberPO>().eq("group_id", po.getId()));
        List<Long> userIds = members.stream().map(VaultGroupMemberPO::getUserId).distinct().toList();
        Map<Long, UserPO> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(UserPO::getId, u -> u));

        List<VaultGroupDTO.MemberInfo> list = new ArrayList<>();
        for (VaultGroupMemberPO m : members) {
            VaultGroupDTO.MemberInfo info = new VaultGroupDTO.MemberInfo();
            info.setUserId(m.getUserId());
            info.setRole(m.getRole());
            UserPO user = userMap.get(m.getUserId());
            if (user != null) {
                info.setDisplayName(user.getDisplayName());
                info.setAvatarUrl(user.getAvatarUrl());
            }
            list.add(info);
        }
        dto.setMembers(list);
        return dto;
    }

    private VaultGroupDTO setupVaultGroup(VaultGroupPO po, Long currentUserId, boolean isAdmin) {
        VaultGroupDTO dto = setupVaultGroup(po);
        if (!isAdmin) {
            boolean inGroup = memberMapper.exists(
                    new QueryWrapper<VaultGroupMemberPO>()
                            .eq("group_id", po.getId()).eq("user_id", currentUserId));
            if (!inGroup) {
                dto.setMembers(List.of());
            }
        }
        return dto;
    }
}
