package me.aloic.apeurival.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.dto.VaultAuthorDTO;
import me.aloic.apeurival.entity.dto.VaultItemDTO;
import me.aloic.apeurival.entity.dto.VaultItemRequest;
import me.aloic.apeurival.service.VaultService;
import me.aloic.apeurival.util.CommonTool;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vault")
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @GetMapping("/authors")
    public Page<VaultAuthorDTO> listAuthors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[GET] handling listAuthors /api/vault/authors");
        return vaultService.listAuthors(page, size);
    }

    @GetMapping
    public Page<VaultItemDTO> listAllVisiblePage(
            @RequestParam(required = false) Long owner,
            @RequestParam(required = false) String authorName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        log.info("[GET] handling list /api/vault owner={} authorName={}", owner, authorName);
        String role = CommonTool.extractRoleCutPrefix(auth);
        Long currentUserId = auth != null ? Long.valueOf(auth.getPrincipal().toString()) : null;
        return vaultService.listVisibleItemsWithCurrentRole(owner, authorName, role, currentUserId, page, size);
    }

    @PostMapping
    public VaultItemDTO create(@RequestBody VaultItemRequest request, Authentication auth) {
        log.info("[POST] handling create /api/vault");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return vaultService.createSingleVaultItem(request, userId);
    }

    @PostMapping("/batch")
    public List<VaultItemDTO> batchCreate(@RequestBody List<VaultItemRequest> requests, Authentication auth) {
        log.info("[POST] handling batchCreate /api/vault/batch size={}", requests.size());
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return vaultService.batchCreate(requests, userId);
    }

    @PutMapping("/{id}")
    public VaultItemDTO update(@PathVariable Long id, @RequestBody VaultItemRequest request,
                                Authentication auth) {
        log.info("[PUT] handling update /api/vault of id {}", id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return vaultService.updateVaultItem(id, request, userId);
    }

    @PutMapping("/batch/visibility")
    public Map<String, Integer> batchSetVisibility(@RequestBody Map<String, Object> body,
                                                     Authentication auth) {
        @SuppressWarnings("unchecked")
        List<Integer> idsRaw = (List<Integer>) body.get("ids");
        List<Long> ids = idsRaw.stream().map(Long::valueOf).toList();
        String visibility = (String) body.get("visibility");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        int count = vaultService.batchSetVisibility(ids, visibility, userId);
        return Map.of("updated", count);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        log.info("[DELETE] handling delete /api/vault of id {}", id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        vaultService.deleteVaultItem(id, userId);
    }

    @DeleteMapping("/batch")
    public Map<String, Integer> batchDelete(@RequestBody Map<String, List<Integer>> body,
                                              Authentication auth) {
        List<Long> ids = body.get("ids").stream().map(Long::valueOf).toList();
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        int count = vaultService.batchDelete(ids, userId);
        return Map.of("deleted", count);
    }
}
