package me.aloic.apeurival.converter;

import me.aloic.apeurival.entity.dto.VaultItemDTO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.VaultItemPO;

public class VaultConverter
{
    public static VaultItemDTO setupVaultItemDTO(VaultItemPO po, UserPO owner) {
        VaultItemDTO dto = new VaultItemDTO();
        dto.setId(po.getId());
        dto.setImageUrl(po.getImageUrl());
        dto.setLabel(po.getLabel());
        dto.setVisibility(po.getVisibility());
        dto.setCreatedAt(po.getCreatedAt().toLocalDate());
        if (po.getAuthorName() != null && !po.getAuthorName().isBlank()) {
            dto.setOwnerName(po.getAuthorName());
        } else if (owner != null) {
            dto.setOwnerName(owner.getDisplayName());
            dto.setOwnerAvatar(owner.getAvatarUrl());
        }
        return dto;
    }
}
