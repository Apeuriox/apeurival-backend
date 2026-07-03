package me.aloic.apeurival.controller;

import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.dto.VaultGroupDTO;
import me.aloic.apeurival.entity.dto.VaultGroupRequest;
import me.aloic.apeurival.enums.RoleEnum;
import me.aloic.apeurival.enums.RoleGroupEnum;
import me.aloic.apeurival.service.VaultGroupService;
import me.aloic.apeurival.util.CommonTool;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vault/groups")
public class VaultGroupController {

    private final VaultGroupService vaultGroupService;

    public VaultGroupController(VaultGroupService vaultGroupService) {
        this.vaultGroupService = vaultGroupService;
    }

    @PostMapping
    public VaultGroupDTO create(@RequestBody VaultGroupRequest request, Authentication auth) {
        log.info("[POST] creating group {}", request.getName());
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        RoleEnum role = CommonTool.extractRole(auth);
        return vaultGroupService.createNewVaultGroup(request, userId, role);
    }

    @GetMapping
    public List<VaultGroupDTO> list(Authentication auth) {
        log.info("[GET] listing all groups");
        Long userId = auth != null ? Long.valueOf(auth.getPrincipal().toString()) : null;
        RoleEnum role = CommonTool.extractRole(auth);
        return vaultGroupService.listAllVaultGroup(userId, role);
    }

    @PutMapping("/{id}")
    public VaultGroupDTO update(@PathVariable Long id, @RequestBody VaultGroupRequest request,
                                 Authentication auth) {
        log.info("[PUT] updating group of id:{}", id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        RoleEnum role = CommonTool.extractRole(auth);
        return vaultGroupService.update(id, request, userId, role);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        log.info("[DELETE] deleting group: {}", id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        RoleEnum role = CommonTool.extractRole(auth);
        vaultGroupService.delete(id, userId, role);
    }

    @PostMapping("/{id}/members")
    public Map<String, Object> addMember(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          Authentication auth) {
        log.info("[POST] adding member to group: {}", id);
        Long targetUserId = Long.valueOf(body.get("userId").toString());
        String memberRole = (String) body.getOrDefault("role", RoleGroupEnum.MEMBER.name());
        Long callerId = Long.valueOf(auth.getPrincipal().toString());
        RoleEnum callerRole = CommonTool.extractRole(auth);
        vaultGroupService.addMember(id, targetUserId, memberRole, callerId, callerRole);
        return Map.of("added", true);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Map<String, Object> removeMember(@PathVariable Long id,
                                             @PathVariable Long userId,
                                             Authentication auth) {
        log.info("[DELETE] deleting member from group: {}", id);
        Long callerId = Long.valueOf(auth.getPrincipal().toString());
        RoleEnum callerRole = CommonTool.extractRole(auth);
        vaultGroupService.removeMember(id, userId, callerId, callerRole);
        return Map.of("removed", true);
    }
}
