package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WorkSummaryDTO {
    private Long id;
    private String title;
    private String type;
    private String coverUrl;
    private List<String> tags;
    private String authorName;
    private LocalDate date;
}
