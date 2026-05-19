package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String profileUrl;
    private String role;
    private LocalDateTime createdAt;
    private List<LinkedAccount> linkedAccounts;

    @Data
    public static class LinkedAccount {
        private String provider;
        private String providerUsername;
        private LocalDateTime linkedAt;
    }
}
