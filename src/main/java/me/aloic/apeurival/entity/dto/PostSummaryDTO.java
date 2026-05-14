package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PostSummaryDTO {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private List<String> tags;
    private LocalDate date;
    private String coverUrl;
}
