package me.aloic.apeurival.entity.dto;

import lombok.Data;

@Data
public class WorkRequest {
    private String title;
    private String description;
    private String type;
    private String contentUrl;
    private String coverUrl;
    private String tags;
    private String authorName;
    private Integer status;
}
