package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WorkDetailDTO {
    private Long id;
    private String title;
    private String description;
    private String type;
    private String coverUrl;
    private List<String> tags;
    private AuthorBrief author;
    private LocalDate date;

    private CodeContent code;
    private ImageContent image;
    private VideoContent video;

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
        private String imageUrl;
        private Integer width;
        private Integer height;
        private String format;
    }

    @Data
    public static class VideoContent {
        private String bvid;
        private String embedUrl;
        private String platform;
    }
}
