package me.aloic.apeurival.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.po.OperationLogPO;
import me.aloic.apeurival.entity.po.UploadMetaPO;
import me.aloic.apeurival.service.AdminService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/operations")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public Page<OperationLogPO> listLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[GET] listing operation logs entityType={} entityId={}", entityType, entityId);
        return adminService.listOperationLogs(entityType, entityId, page, size);
    }

    @PostMapping("/{logId}/revert")
    public Map<String, Object> revertLog(@PathVariable Long logId, Authentication auth) {
        log.info("[POST] reverting operation log id={}", logId);
        Long operatorId = Long.valueOf(auth.getPrincipal().toString());
        return adminService.revertOperation(logId, operatorId);
    }

    @GetMapping("/uploads/unbound")
    public List<UploadMetaPO> listUnboundUploads() {
        log.info("[GET] listing unbound uploads");
        return adminService.listUnboundUploads();
    }

    @DeleteMapping("/uploads/clean")
    public Map<String, Object> cleanExpiredUploads() {
        log.info("[DELETE] cleaning expired unbound uploads");
        int removed = adminService.bindAndCleanUploads();
        return Map.of("removed", removed);
    }
}
