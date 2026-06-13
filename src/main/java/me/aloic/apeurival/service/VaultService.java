package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.VaultAuthorDTO;
import me.aloic.apeurival.entity.dto.VaultItemDTO;
import me.aloic.apeurival.entity.dto.VaultItemRequest;

import java.util.List;

public interface VaultService {

    Page<VaultAuthorDTO> listAuthors(Long groupId, int page, int size, Long currentUserId, String userRole);

    Page<VaultItemDTO> listVisibleItemsWithCurrentRole(Long ownerId, String authorName,
                                        Long groupId,
                                        String userRole, Long currentUserId,
                                        int page, int size);

    VaultItemDTO createSingleVaultItem(VaultItemRequest request, Long ownerId);

    List<VaultItemDTO> batchCreate(List<VaultItemRequest> requests, Long ownerId);

    VaultItemDTO updateVaultItem(Long id, VaultItemRequest request, Long userId);

    void deleteVaultItem(Long id, Long userId);

    int batchDelete(List<Long> ids, Long userId);

    int batchSetVisibility(List<Long> ids, String visibility, Long userId);
}
