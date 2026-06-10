package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VaultItemDTO {
    private Long id;
    private String imageUrl;
    private String label;
    private String visibility;
    private LocalDate createdAt;
    private String ownerName;
    private String ownerAvatar;
}
