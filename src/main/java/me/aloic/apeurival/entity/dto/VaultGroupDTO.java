package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VaultGroupDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private List<MemberInfo> members;

    @Data
    public static class MemberInfo {
        private Long userId;
        private String displayName;
        private String avatarUrl;
        private String role;
    }
}
