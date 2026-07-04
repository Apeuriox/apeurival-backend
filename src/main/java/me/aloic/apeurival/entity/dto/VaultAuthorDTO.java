package me.aloic.apeurival.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VaultAuthorDTO {
    private Long ownerId;
    private String authorName;
    private String avatarUrl;
    private int itemCount;
    private boolean external;
    private List<GroupInfo> groups = new ArrayList<>();

    @Data
    public static class GroupInfo {
        private Long groupId;
        private String groupName;
        private int itemCount;
    }
}
