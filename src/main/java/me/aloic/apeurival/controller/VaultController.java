package me.aloic.apeurival.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.dto.VaultItemDTO;
import me.aloic.apeurival.entity.dto.VaultItemRequest;
import me.aloic.apeurival.service.VaultService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/vault")
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @GetMapping
    public Page<VaultItemDTO> listAllVisiblePage(
            @RequestParam Long owner,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        log.info("[GET] handling list /api/vault");
        String role = extractRole(auth);
        Long currentUserId = auth != null ? Long.valueOf(auth.getPrincipal().toString()) : null;
        return vaultService.listVisibleItemsWithCurrentRole(owner, role, currentUserId, page, size);
    }

    @PostMapping
    public VaultItemDTO create(@RequestBody VaultItemRequest request, Authentication auth) {
        log.info("[POST] handling create /api/vault");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return vaultService.create(request, userId);
    }

    @PutMapping("/{id}")
    public VaultItemDTO update(@PathVariable Long id, @RequestBody VaultItemRequest request,
                                Authentication auth) {
        log.info("[PUT] handling update /api/vault of id {}",id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return vaultService.update(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        log.info("[DELETE] handling delete /api/vault of id {}",id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        vaultService.delete(id, userId);
    }

    private static String extractRole(Authentication auth) {
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse(null);
    }
}
