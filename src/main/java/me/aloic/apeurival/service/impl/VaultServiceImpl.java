package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.converter.VaultConverter;
import me.aloic.apeurival.entity.dto.VaultAuthorDTO;
import me.aloic.apeurival.entity.dto.VaultItemDTO;
import me.aloic.apeurival.entity.dto.VaultItemRequest;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.VaultGroupMemberMapper;
import me.aloic.apeurival.entity.mapper.VaultItemMapper;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.VaultGroupMemberPO;
import me.aloic.apeurival.entity.po.VaultItemPO;
import me.aloic.apeurival.enums.EntityTypeEnum;
import me.aloic.apeurival.enums.RoleEnum;
import me.aloic.apeurival.service.OperationLogService;
import me.aloic.apeurival.service.VaultService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VaultServiceImpl implements VaultService {

    private final VaultItemMapper vaultItemMapper;
    private final UserMapper userMapper;
    private final VaultGroupMemberMapper groupMemberMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public VaultServiceImpl(VaultItemMapper vaultItemMapper,
                            VaultGroupMemberMapper groupMemberMapper,
                            UserMapper userMapper,
                            OperationLogService operationLogService,
                            ObjectMapper objectMapper) {
        this.vaultItemMapper = vaultItemMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    //it's broken id fix later
    @Override
    public Page<VaultAuthorDTO> listAuthors(Long groupId, int page, int size, Long currentUserId, RoleEnum userRole) {
        if (groupId != null) {
            checkGroupAccess(groupId, currentUserId, userRole);
        }
        Page<Map<String, Object>> rawPage = new Page<>(page, size);
        Page<Map<String, Object>> raw = vaultItemMapper.countByAuthorsPage(rawPage,groupId);

        List<VaultAuthorDTO> records = new ArrayList<>();
        for (Map<String, Object> row : raw.getRecords()) {
            VaultAuthorDTO dto = new VaultAuthorDTO();
            Object ownerIdObj = row.get("owner_id");
            String authorName = (String) row.get("author_name");
            Long id = ownerIdObj != null ? ((Number) ownerIdObj).longValue() : null;
            dto.setOwnerId(id);
            dto.setExternal(authorName != null && !authorName.isBlank());
            dto.setItemCount(((Number) row.get("item_count")).intValue());
            if (authorName != null && !authorName.isBlank()) {
                dto.setAuthorName(authorName);
            } else if (dto.getOwnerId() != null) {
                UserPO user = userMapper.selectById(dto.getOwnerId());
                if (user != null) {
                    dto.setAuthorName(user.getDisplayName());
                    dto.setAvatarUrl(user.getAvatarUrl());
                }
            }
            records.add(dto);
        }
        Page<VaultAuthorDTO> result = new Page<>(page, size, raw.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public Page<VaultItemDTO> listVisibleItemsWithCurrentRole(Long ownerId, String authorName,
                                                               Long groupId,
                                                               RoleEnum userRole, Long currentUserId,
                                                               int page, int size) {
        if (groupId != null) {
            checkGroupAccess(groupId, currentUserId, userRole);
        }
        boolean isOwner = currentUserId != null && currentUserId.equals(ownerId);
        Page<VaultItemPO> poPage = new Page<>(page, size);
        List<String> visibilities;
        if (userRole == null) {
            visibilities = List.of("PUBLIC");
        } else {
            visibilities = userRole.visibleVisibilities(isOwner);
        }
        Page<VaultItemPO> result;
        if (groupId != null) {
            result = vaultItemMapper.selectGroupItemsPage(poPage, groupId, ownerId , authorName, visibilities);
        } else {
            result = vaultItemMapper.selectNonGroupItemsPage(poPage, ownerId, authorName, visibilities);
        }

        UserPO owner = ownerId != null ? userMapper.selectById(ownerId) : null;
        Page<VaultItemDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(po -> VaultConverter.setupVaultItemDTO(po, owner))
                .toList());
        return dtoPage;
    }

    @Override
    public VaultItemDTO createSingleVaultItem(VaultItemRequest req, Long ownerId, RoleEnum userRole) {
        validateAuthorName(req.getAuthorName(), userRole);
        VaultItemPO po = insertOne(req, ownerId);
        operationLogService.logCreate(EntityTypeEnum.VAULT_ITEM.getCode(), po.getId(), ownerId, po);
        return VaultConverter.setupVaultItemDTO(po, userMapper.selectById(ownerId));
    }

    @Override
    @Transactional
    public List<VaultItemDTO> batchCreate(List<VaultItemRequest> requests, Long ownerId, RoleEnum userRole) {
        validateAuthorName(requests.getFirst().getAuthorName(), userRole);
        UserPO owner = userMapper.selectById(ownerId);
        List<VaultItemDTO> result = new ArrayList<>();
        for (VaultItemRequest req : requests) {
            VaultItemPO po = insertOne(req, ownerId);
            result.add(VaultConverter.setupVaultItemDTO(po, owner));
            operationLogService.logCreate(EntityTypeEnum.VAULT_ITEM.getCode(), po.getId(), ownerId, po);
            log.info("created vault item in BATCH: {}",po.getLabel());
        }
        log.info("Successfully created {} vault items.",result.size());
        return result;
    }

    @Override
    public VaultItemDTO getVaultItemById(Long id, Long currentUserId, RoleEnum userRole) {
        VaultItemPO po = vaultItemMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vault item not found");
        }
        boolean isOwner = currentUserId != null && currentUserId.equals(po.getOwnerId());
        boolean isAdmin = userRole != null && userRole.isAdmin();
        if (isOwner || isAdmin) {
            return VaultConverter.setupVaultItemDTO(po, userMapper.selectById(po.getOwnerId()));
        }
        if (po.getGroupId() != null) {
            checkGroupAccess(po.getGroupId(), currentUserId, userRole);
        } else {
            checkVisibility(po.getVisibility(), userRole);
        }
        return VaultConverter.setupVaultItemDTO(po, userMapper.selectById(po.getOwnerId()));
    }

    private void checkVisibility(String visibility, RoleEnum userRole) {
        List<String> visible = userRole != null
                ? userRole.visibleVisibilities(false)
                : List.of("PUBLIC");
        if (visibility == null || !visible.contains(visibility.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    @Override
    public VaultItemDTO updateVaultItem(Long id, VaultItemRequest req, Long userId) {
        VaultItemPO po = vaultItemMapper.selectById(id);
        if (po == null) {
            log.warn("Updated failed cuz no such vault item: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vault item not found");
        }
        checkOwnership(po, userId);
        VaultItemPO oldPo = clonePo(po);
        if (req.getImageUrl() != null) po.setImageUrl(req.getImageUrl());
        if (req.getLabel() != null) po.setLabel(req.getLabel());
        if (req.getVisibility() != null) po.setVisibility(req.getVisibility().toUpperCase());
        vaultItemMapper.updateById(po);
        log.info("Successfully updated vault item: {}", id);
        operationLogService.logUpdate(EntityTypeEnum.VAULT_ITEM.getCode(), id, userId, po, oldPo);
        return VaultConverter.setupVaultItemDTO(po, userMapper.selectById(po.getOwnerId()));
    }

    @Override
    public void deleteVaultItem(Long id, Long userId) {
        VaultItemPO po = vaultItemMapper.selectById(id);
        if (po == null) {
            log.warn("Deletion failed cuz no such vault item: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vault item not found");
        }
        checkOwnership(po, userId);
        vaultItemMapper.deleteById(id);
        operationLogService.logDelete(EntityTypeEnum.VAULT_ITEM.getCode(), id, userId, po);
        log.info("Successfully deleted vault item: {}", id);
    }

    @Override
    @Transactional
    public int batchDelete(List<Long> ids, Long userId) {
        int count = 0;
        for (Long id : ids) {
            VaultItemPO po = vaultItemMapper.selectById(id);
            if (po == null) continue;
            checkOwnership(po, userId);
            vaultItemMapper.deleteById(id);
            operationLogService.logDelete(EntityTypeEnum.VAULT_ITEM.getCode(), id, userId, po);
            count++;
            log.info("deleting vault item in BATCH: {}",po.getLabel());
        }
        log.info("Successfully deleted {} vault items.", count);
        return count;
    }

    @Override
    @Transactional
    public int batchSetVisibility(List<Long> ids, String visibility, Long userId) {
        int count = 0;
        String vis = visibility.toUpperCase();
        for (Long id : ids) {
            VaultItemPO po = vaultItemMapper.selectById(id);
            if (po == null) continue;
            checkOwnership(po, userId);
            po.setVisibility(vis);
            vaultItemMapper.updateById(po);
            count++;
        }
        return count;
    }

    private VaultItemPO insertOne(VaultItemRequest req, Long ownerId) {
        VaultItemPO po = new VaultItemPO();
        po.setOwnerId(ownerId);
        po.setAuthorName(req.getAuthorName());
        po.setImageUrl(req.getImageUrl());
        po.setLabel(req.getLabel());
        po.setGroupId(req.getGroupId());
        po.setVisibility(req.getVisibility() != null ? req.getVisibility().toUpperCase() : "PUBLIC");
        po.setCreatedAt(LocalDateTime.now());
        vaultItemMapper.insert(po);
        return po;
    }

    private void checkOwnership(VaultItemPO po, Long userId) {
        if (po.getOwnerId() != null && po.getOwnerId().equals(userId)) return;
        UserPO caller = userMapper.selectById(userId);
        if (caller != null && RoleEnum.ADMIN == RoleEnum.fromString(caller.getRole())) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own vault items");
    }

    private void checkGroupAccess(Long groupId, Long userId, RoleEnum userRole) {
        if (userRole != null && userRole.isAdmin()) return;
        if (groupMemberMapper.exists(
                new QueryWrapper<VaultGroupMemberPO>()
                        .eq("group_id", groupId).eq("user_id", userId))) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
    }

    private void validateAuthorName(String authorName, RoleEnum userRole) {
        if (authorName != null && !authorName.isBlank()
                && (userRole == null || !userRole.canAssignExternalAuthor())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only EDITOR/ADMIN can assign external author");
        }
    }

    private VaultItemPO clonePo(VaultItemPO po) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(po), VaultItemPO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone entity", e);
        }
    }
}
