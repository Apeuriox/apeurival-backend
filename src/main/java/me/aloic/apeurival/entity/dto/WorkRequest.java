package me.aloic.apeurival.entity.dto;

import lombok.Data;

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
    private String imageUrl;
    private Integer width;
    private Integer height;
    private String format;

    // VIDEO
    private String bvid;
    private String platform;
}
