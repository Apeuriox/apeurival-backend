package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WorkDetailDTO {
    private Long id;
    private String title;
    private String description;
    private String contentMd;
    private String type;
    private String coverUrl;
    private List<String> tags;
    private AuthorBrief author;
    private LocalDate date;

    private CodeContent code;
    private ImageContent image;
    private VideoContent video;
    private TimelineContent timeline;

    @Data
    public static class AuthorBrief {
        private Long id;
        private String displayName;
        private String avatarUrl;
        private String profileUrl;
    }

    @Data
    public static class CodeContent {
        private String repoUrl;
        private List<String> languages;
        private Integer stars;
    }

    @Data
    public static class ImageContent {
        private Integer width;
        private Integer height;
        private String format;
        private List<ImageItem> images;
    }

    @Data
    public static class ImageItem {
        private String imageUrl;
        private String label;
        private Integer sortOrder;
    }

    @Data
    public static class VideoContent {
        private String bvid;
        private String embedUrl;
        private String platform;
    }

    @Data
    public static class TimelineContent {
        private List<Moment> moments;
    }

    @Data
    public static class Moment {
        private String imageUrl;
        private String content;
        private LocalDate momentTime;
        private Integer sortOrder;
    }
}
