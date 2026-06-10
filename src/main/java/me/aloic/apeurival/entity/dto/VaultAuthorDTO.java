package me.aloic.apeurival.entity.dto;

import lombok.Data;

@Data
public class VaultAuthorDTO {
    private Long ownerId;
    private String authorName;
    private String avatarUrl;
    private int itemCount;
}
