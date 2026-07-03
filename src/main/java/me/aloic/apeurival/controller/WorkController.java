package me.aloic.apeurival.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.dto.WorkDetailDTO;
import me.aloic.apeurival.entity.dto.WorkRequest;
import me.aloic.apeurival.entity.dto.WorkSummaryDTO;
import me.aloic.apeurival.service.WorkService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/works")
public class WorkController {

    private final WorkService workService;

    public WorkController(WorkService workService) {
        this.workService = workService;
    }

    @GetMapping
    public Page<WorkSummaryDTO> listWorks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("[GET] handling getListWorks /api/works");
        return workService.listPublishedWorks(type, authorId, sort, page, size);
    }

    @GetMapping("/{id}")
    public WorkDetailDTO getWork(@PathVariable Long id) {
        log.info("[GET] handling getDetailWork /api/works/{}",id);
        return workService.getWork(id);
    }

    @PostMapping
    public WorkDetailDTO createWork(@RequestBody WorkRequest request, Authentication auth) {
        log.info("[POST] handling createWork /api/works");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return workService.createWork(request, userId);
    }

    @PutMapping("/{id}")
    public WorkDetailDTO updateWork(@PathVariable Long id, @RequestBody WorkRequest request,
                                     Authentication auth) {
        log.info("[PUT] handling updateWork /api/works/{}",id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return workService.updateWork(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public void deleteWork(@PathVariable Long id, Authentication auth) {
        log.info("[DELETE] handling deleteWork /api/works/{}",id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        workService.deleteWork(id, userId);
    }
}
