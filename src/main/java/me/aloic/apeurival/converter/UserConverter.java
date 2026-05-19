package me.aloic.apeurival.converter;

import me.aloic.apeurival.entity.dto.UserDTO;
import me.aloic.apeurival.entity.po.UserOAuthPO;
import me.aloic.apeurival.entity.po.UserPO;

import java.util.Collections;
import java.util.List;

public final class UserConverter {

    private UserConverter() {}

    public static UserDTO toDTO(UserPO po, List<UserOAuthPO> linkedAccounts) {
        UserDTO dto = new UserDTO();
        dto.setId(po.getId());
        dto.setUsername(po.getUsername());
        dto.setEmail(po.getEmail());
        dto.setDisplayName(po.getDisplayName());
        dto.setAvatarUrl(po.getAvatarUrl());
        dto.setProfileUrl(po.getProfileUrl());
        dto.setRole(po.getRole());
        dto.setCreatedAt(po.getCreatedAt());

        if (linkedAccounts != null && !linkedAccounts.isEmpty()) {
            dto.setLinkedAccounts(linkedAccounts.stream().map(l -> {
                UserDTO.LinkedAccount la = new UserDTO.LinkedAccount();
                la.setProvider(l.getProvider());
                la.setProviderUsername(l.getProviderUsername());
                la.setLinkedAt(l.getLinkedAt());
                return la;
            }).toList());
        } else {
            dto.setLinkedAccounts(Collections.emptyList());
        }
        return dto;
    }
}
