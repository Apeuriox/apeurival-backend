package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.VaultItemDTO;
import me.aloic.apeurival.entity.dto.VaultItemRequest;

public interface VaultService {

    Page<VaultItemDTO> listVisibleItemsWithCurrentRole(Long ownerId, String userRole, Long currentUserId, int page, int size);

    VaultItemDTO create(VaultItemRequest request, Long ownerId);

    VaultItemDTO update(Long id, VaultItemRequest request, Long userId);

    void delete(Long id, Long userId);
}
