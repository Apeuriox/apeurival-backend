package me.aloic.apeurival.service;

import me.aloic.apeurival.entity.dto.VaultGroupDTO;
import me.aloic.apeurival.entity.dto.VaultGroupRequest;
import me.aloic.apeurival.enums.RoleEnum;

import java.util.List;

public interface VaultGroupService {

    VaultGroupDTO createNewVaultGroup(VaultGroupRequest request, Long userId, RoleEnum userRole);

    List<VaultGroupDTO> listAllVaultGroup(Long currentUserId, RoleEnum userRole);

    VaultGroupDTO update(Long id, VaultGroupRequest request, Long userId, RoleEnum userRole);

    void delete(Long id, Long userId, RoleEnum userRole);

    void addMember(Long groupId, Long userId, String role, Long callerId, RoleEnum callerRole);

    void removeMember(Long groupId, Long userId, Long callerId, RoleEnum callerRole);
}
