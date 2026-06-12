package me.aloic.apeurival.service;

import me.aloic.apeurival.entity.dto.VaultGroupDTO;
import me.aloic.apeurival.entity.dto.VaultGroupRequest;

import java.util.List;

public interface VaultGroupService {

    VaultGroupDTO createNewVaultGroup(VaultGroupRequest request, Long userId, String userRole);

    List<VaultGroupDTO> listAllVaultGroup();

    VaultGroupDTO update(Long id, VaultGroupRequest request, Long userId, String userRole);

    void delete(Long id, Long userId, String userRole);

    void addMember(Long groupId, Long userId, String role, Long callerId, String callerRole);

    void removeMember(Long groupId, Long userId, Long callerId, String callerRole);
}
