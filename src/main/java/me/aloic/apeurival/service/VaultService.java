package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.VaultAuthorDTO;
import me.aloic.apeurival.entity.dto.VaultItemDTO;
import me.aloic.apeurival.entity.dto.VaultItemRequest;
import me.aloic.apeurival.enums.RoleEnum;

import java.util.List;

public interface VaultService {

    Page<VaultAuthorDTO> listAuthors(Long groupId, int page, int size, Long currentUserId, RoleEnum userRole);

    Page<VaultItemDTO> listVisibleItemsWithCurrentRole(Long ownerId, String authorName,
                                        Long groupId,
                                        RoleEnum userRole, Long currentUserId,
                                        int page, int size);

    VaultItemDTO createSingleVaultItem(VaultItemRequest request, Long ownerId, RoleEnum userRole);

    List<VaultItemDTO> batchCreate(List<VaultItemRequest> requests, Long ownerId, RoleEnum userRole);

    VaultItemDTO updateVaultItem(Long id, VaultItemRequest request, Long userId);

    void deleteVaultItem(Long id, Long userId);

    int batchDelete(List<Long> ids, Long userId);

    int batchSetVisibility(List<Long> ids, String visibility, Long userId);
}
