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
    private String contentUrl;
    private String embedUrl;
    private String coverUrl;
    private List<String> tags;
    private String authorName;
    private LocalDate date;
}
