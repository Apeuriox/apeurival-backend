package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WorkRequest {
    private String title;
    private String description;
    private String type;
    private String coverUrl;
    private String tags;
    private Integer status;

    // CODE
    private String repoUrl;
    private String languages;

    // IMAGE
    private Integer width;
    private Integer height;
    private String format;
    private List<ImageRequest> images;

    // VIDEO
    private String bvid;
    private String platform;

    // TIMELINE (any type)
    private List<MomentRequest> moments;

    @Data
    public static class ImageRequest {
        private String imageUrl;
        private String label;
    }

    @Data
    public static class MomentRequest {
        private String imageUrl;
        private String content;
        private LocalDate momentTime;
    }
}
