package me.aloic.apeurival.controller;

import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.config.UploadPathConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final Path uploadDir;
    private final long maxSize;
    private final List<String> allowedTypes;

    public UploadController(
            UploadPathConfig uploadPathConfig,
            @Value("${app.upload.image-max-size}") long maxSize,
            @Value("${app.upload.allowed-types}") List<String> allowedTypes) {
        this.maxSize = maxSize;
        this.allowedTypes = allowedTypes;
        this.uploadDir = Paths.get(uploadPathConfig.resolve(), "images").toAbsolutePath();
    }

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("[POST] handling uploadImage /api/upload");
        if (file.isEmpty()) {
            log.warn("Target image upload was null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > maxSize) {
            log.warn("Target image upload has maxed out size");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File too large, max " + maxSize / 1048576 + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            log.warn("Target image upload cant be accepted, unsupported file type: {}",contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type: " + contentType);
        }

        try {
            byte[] bytes = file.getBytes();
            String hash = sha256(bytes);
            String ext = extension(contentType);
            String filename = hash + ext;
            Path target = uploadDir.resolve(filename);

            if (Files.notExists(target)) {
                Files.write(target, bytes);
                log.info("Image upload saved: {}", filename);
            } else {
                log.info("Image upload dedup: {} already exists", filename);
            }

            return ResponseEntity.ok(Map.of("url", "/uploads/images/" + filename));
        } catch (IOException e) {
            log.warn("IOException caught: {}",e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String extension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> "";
        };
    }
}
