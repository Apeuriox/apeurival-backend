package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import me.aloic.apeurival.entity.mapper.VaultGroupMemberMapper;
import me.aloic.apeurival.entity.po.VaultGroupMemberPO;
import me.aloic.apeurival.entity.po.VaultItemPO;
import me.aloic.apeurival.enums.RoleEnum;
import me.aloic.apeurival.enums.VaultVisibility;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class VaultAccessPolicy {
    private final VaultGroupMemberMapper groupMemberMapper;

    public VaultAccessPolicy(VaultGroupMemberMapper groupMemberMapper) {
        this.groupMemberMapper = groupMemberMapper;
    }

    public void requireVaultAccess(RoleEnum role) {
        if (role == null || !role.isOsuOrAbove()) deny("Permission Denied");
    }

    public void requireGroupBrowse(Long groupId, Long userId, RoleEnum role) {
        requireVaultAccess(role);
        if (role.isAtLeastLibrarian() || isGroupMember(groupId, userId)) return;
        deny("You are not a member of this group");
    }

    public void requireGroupWrite(Long groupId, Long userId, RoleEnum role) {
        requireVaultWrite(role);
        if (role.isAdmin() || isGroupMember(groupId, userId)) return;
        deny("You must be a group member to add content");
    }

    public void requireVaultWrite(RoleEnum role) {
        if (role == null || !role.isAtLeastEditor()) {
            deny("Vault write access requires EDITOR role or above");
        }
    }

    public void requireItemRead(VaultItemPO item, Long userId, RoleEnum role) {
        requireVaultAccess(role);
        if (userId != null && userId.equals(item.getOwnerId())) return;
        if (role.isAdmin()) return;
        if (item.getGroupId() != null) requireGroupBrowse(item.getGroupId(), userId, role);
        if (!isVisibilityReadable(item.getVisibility(), role)) deny("Access denied");
    }

    public List<String> visibleDatabaseValues(RoleEnum role) {
        requireVaultAccess(role);
        return VaultVisibility.visibleDatabaseValues(role);
    }

    public String normalizeWritableVisibility(String visibility) {
        try {
            return VaultVisibility.fromString(visibility == null ? "PUBLIC" : visibility).name();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Visibility must be PUBLIC, RESTRICTED, EDITOR_ONLY or PRIVATE");
        }
    }

    private boolean isVisibilityReadable(String visibility, RoleEnum role) {
        if ("MEMBERS".equalsIgnoreCase(visibility)) return role.isOsuOrAbove();
        try {
            return VaultVisibility.fromString(visibility).isVisibleTo(role);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isGroupMember(Long groupId, Long userId) {
        return userId != null && groupMemberMapper.exists(new QueryWrapper<VaultGroupMemberPO>()
                .eq("group_id", groupId).eq("user_id", userId));
    }

    private void deny(String message) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
