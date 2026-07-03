package me.aloic.apeurival.entity.dto;

import lombok.Data;
import me.aloic.apeurival.enums.PostCategoryEnum;

import java.time.LocalDate;
import java.util.List;

@Data
public class PostDetailDTO {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String contentMd;
    private PostCategoryEnum category;
    private List<String> tags;
    private LocalDate date;
    private String coverUrl;
    private AuthorBrief author;
    private PostDetailDTO prev;
    private PostDetailDTO next;

    @Data
    public static class AuthorBrief {
        private Long id;
        private String displayName;
        private String avatarUrl;
        private String profileUrl;
    }
}
