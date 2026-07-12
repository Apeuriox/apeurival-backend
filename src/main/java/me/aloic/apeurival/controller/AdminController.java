package me.aloic.apeurival.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.po.OperationLogPO;
import me.aloic.apeurival.entity.po.UploadMetaPO;
import me.aloic.apeurival.entity.dto.AdminUserRoleRequest;
import me.aloic.apeurival.entity.dto.UserDTO;
import me.aloic.apeurival.service.AdminService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/operations")
    public Page<OperationLogPO> listLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[GET] listing operation logs entityType={} entityId={}", entityType, entityId);
        return adminService.listOperationLogs(entityType, entityId, page, size);
    }

    @PostMapping("/operations/{logId}/revert")
    public Map<String, Object> revertLog(@PathVariable Long logId, Authentication auth) {
        log.info("[POST] reverting operation log id={}", logId);
        Long operatorId = Long.valueOf(auth.getPrincipal().toString());
        return adminService.revertOperation(logId, operatorId);
    }

    @GetMapping({"/uploads/unbound", "/operations/uploads/unbound"})
    public List<UploadMetaPO> listUnboundUploads() {
        log.info("[GET] listing unbound uploads");
        return adminService.listUnboundUploads();
    }

    @DeleteMapping({"/uploads/clean", "/operations/uploads/clean"})
    public Map<String, Object> cleanExpiredUploads() {
        log.info("[DELETE] cleaning expired unbound uploads");
        int removed = adminService.bindAndCleanUploads();
        return Map.of("removed", removed);
    }

    @PostMapping("/users/{userId}/invalidate-tokens")
    public Map<String, Object> invalidateUserTokens(@PathVariable Long userId) {
        log.info("[POST] invalidating all tokens for user {}", userId);
        adminService.invalidateUserTokens(userId);
        return Map.of("invalidated", true, "userId", userId);
    }

    @GetMapping("/users")
    public Page<UserDTO> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[GET] listing users query={} role={}", query, role);
        return adminService.listUsers(query, role, page, size);
    }

    @GetMapping("/users/{userId}")
    public UserDTO getUser(@PathVariable Long userId) {
        log.info("[GET] retrieving user {}", userId);
        return adminService.getUser(userId);
    }

    @PutMapping("/users/{userId}/role")
    public UserDTO updateUserRole(@PathVariable Long userId,
                                  @RequestBody AdminUserRoleRequest request,
                                  Authentication auth) {
        Long operatorId = Long.valueOf(auth.getPrincipal().toString());
        log.info("[PUT] admin {} updating user {} role", operatorId, userId);
        return adminService.updateUserRole(userId, request.getRole(), operatorId);
    }
}
