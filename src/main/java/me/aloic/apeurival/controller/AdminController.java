package me.aloic.apeurival.controller;

import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.scheduled.FileCleanupScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final FileCleanupScheduler cleanupScheduler;

    public AdminController(FileCleanupScheduler cleanupScheduler) {
        this.cleanupScheduler = cleanupScheduler;
    }

    @GetMapping("/orphan-files")
    public ResponseEntity<Map<String, Object>> previewOrphans() {
        List<String> orphans = cleanupScheduler.listOrphanFiles();
        log.info("Orphan preview: {} files found", orphans.size());
        return ResponseEntity.ok(Map.of(
                "count", orphans.size(),
                "files", orphans
        ));
    }

    @PostMapping("/cleanup-files")
    public ResponseEntity<Map<String, Object>> cleanupFiles() {
        int removed = cleanupScheduler.cleanOrphanFiles();
        log.info("Manual cleanup: {} files removed", removed);
        return ResponseEntity.ok(Map.of("removed", removed));
    }
}
