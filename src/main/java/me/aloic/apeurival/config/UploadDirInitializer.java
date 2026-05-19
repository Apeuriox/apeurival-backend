package me.aloic.apeurival.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class UploadDirInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UploadDirInitializer.class);

    private final Path imagesDir;

    public UploadDirInitializer(UploadPathConfig uploadPathConfig) {
        this.imagesDir = Paths.get(uploadPathConfig.resolve(), "images").toAbsolutePath();
    }

    @Override
    public void run(String... args) throws Exception {
        if (Files.notExists(imagesDir)) {
            Files.createDirectories(imagesDir);
            log.info("Created upload directory: {}", imagesDir);
        } else {
            log.info("Upload directory exists: {}", imagesDir);
        }
    }
}
