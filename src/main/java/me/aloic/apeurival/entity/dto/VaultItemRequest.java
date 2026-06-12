package me.aloic.apeurival.entity.dto;

import lombok.Data;

@Data
public class VaultItemRequest {
    private String imageUrl;
    private String label;
    private String visibility;
    private String authorName;
    private Long groupId;
}
