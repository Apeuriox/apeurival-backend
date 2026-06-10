package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.VaultItemDTO;
import me.aloic.apeurival.entity.dto.VaultItemRequest;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.VaultItemMapper;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.VaultItemPO;
import me.aloic.apeurival.service.VaultService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VaultServiceImpl implements VaultService {

    private final VaultItemMapper vaultItemMapper;
    private final UserMapper userMapper;

    public VaultServiceImpl(VaultItemMapper vaultItemMapper, UserMapper userMapper) {
        this.vaultItemMapper = vaultItemMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Page<VaultItemDTO> listVisibleItemsWithCurrentRole(Long ownerId, String userRole, Long currentUserId,
                                                              int page, int size) {
        Page<VaultItemPO> poPage = new Page<>(page, size);
        QueryWrapper<VaultItemPO> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_id", ownerId);
        applyVisibility(wrapper, userRole, currentUserId, ownerId);
        wrapper.orderByDesc("created_at");

        Page<VaultItemPO> result = vaultItemMapper.selectPage(poPage, wrapper);
        UserPO owner = userMapper.selectById(ownerId);
        List<VaultItemDTO> dtoList = result.getRecords().stream()
                .map(po -> toDTO(po, owner))
                .toList();

        Page<VaultItemDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    public VaultItemDTO create(VaultItemRequest req, Long ownerId) {
        VaultItemPO po = new VaultItemPO();
        po.setOwnerId(ownerId);
        po.setImageUrl(req.getImageUrl());
        po.setLabel(req.getLabel());
        po.setVisibility(req.getVisibility() != null ? req.getVisibility().toUpperCase() : "PUBLIC");
        po.setCreatedAt(LocalDateTime.now());
        vaultItemMapper.insert(po);
        return toDTO(po, userMapper.selectById(ownerId));
    }

    @Override
    public VaultItemDTO update(Long id, VaultItemRequest req, Long userId) {
        VaultItemPO po = vaultItemMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vault item not found");
        }
        checkOwnership(po, userId);
        if (req.getImageUrl() != null) po.setImageUrl(req.getImageUrl());
        if (req.getLabel() != null) po.setLabel(req.getLabel());
        if (req.getVisibility() != null) po.setVisibility(req.getVisibility().toUpperCase());
        vaultItemMapper.updateById(po);
        return toDTO(po, userMapper.selectById(po.getOwnerId()));
    }

    @Override
    public void delete(Long id, Long userId) {
        VaultItemPO po = vaultItemMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vault item not found");
        }
        checkOwnership(po, userId);
        vaultItemMapper.deleteById(id);
    }

    private void checkOwnership(VaultItemPO po, Long userId) {
        if (po.getOwnerId().equals(userId)) return;
        UserPO caller = userMapper.selectById(userId);
        if (caller != null && "ADMIN".equals(caller.getRole())) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own vault items");
    }

    private void applyVisibility(QueryWrapper<VaultItemPO> wrapper,
                                  String userRole, Long currentUserId, Long ownerId) {
        boolean isOwner = currentUserId != null && currentUserId.equals(ownerId);
        boolean isAdmin = "ADMIN".equals(userRole);
        boolean isEditor = "EDITOR".equals(userRole);
        boolean isLoggedIn = currentUserId != null;

        if (isOwner || isAdmin) {
            // owner/admin sees everything — no extra filter
            return;
        }
        if (isEditor) {
            wrapper.in("visibility", "PUBLIC", "MEMBERS", "RESTRICTED");
        } else if (isLoggedIn) {
            wrapper.in("visibility", "PUBLIC", "MEMBERS");
        } else {
            wrapper.eq("visibility", "PUBLIC");
        }
    }

    private static VaultItemDTO toDTO(VaultItemPO po, UserPO owner) {
        VaultItemDTO dto = new VaultItemDTO();
        dto.setId(po.getId());
        dto.setImageUrl(po.getImageUrl());
        dto.setLabel(po.getLabel());
        dto.setVisibility(po.getVisibility());
        dto.setCreatedAt(po.getCreatedAt().toLocalDate());
        if (owner != null) {
            dto.setOwnerName(owner.getDisplayName());
            dto.setOwnerAvatar(owner.getAvatarUrl());
        }
        return dto;
    }
}
