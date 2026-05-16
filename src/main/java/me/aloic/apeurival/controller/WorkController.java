package me.aloic.apeurival.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.WorkDetailDTO;
import me.aloic.apeurival.entity.dto.WorkRequest;
import me.aloic.apeurival.entity.dto.WorkSummaryDTO;
import me.aloic.apeurival.service.WorkService;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return workService.listPublishedWorks(type, page, size);
    }

    @GetMapping("/{id}")
    public WorkDetailDTO getWork(@PathVariable Long id) {
        return workService.getWork(id);
    }

    @PostMapping
    public WorkDetailDTO createWork(@RequestBody WorkRequest request) {
        return workService.createWork(request);
    }

    @PutMapping("/{id}")
    public WorkDetailDTO updateWork(@PathVariable Long id, @RequestBody WorkRequest request) {
        return workService.updateWork(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteWork(@PathVariable Long id) {
        workService.deleteWork(id);
    }
}
