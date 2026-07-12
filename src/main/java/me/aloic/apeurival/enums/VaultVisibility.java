package me.aloic.apeurival.enums;

import java.util.ArrayList;
import java.util.List;

public enum VaultVisibility {
    PUBLIC(RoleEnum.OSU),
    RESTRICTED(RoleEnum.LIBRARIAN),
    EDITOR_ONLY(RoleEnum.EDITOR),
    PRIVATE(RoleEnum.ADMIN);

    private final RoleEnum minimumRole;

    VaultVisibility(RoleEnum minimumRole) {
        this.minimumRole = minimumRole;
    }

    public boolean isVisibleTo(RoleEnum role) {
        return role != null && role.isAtLeast(minimumRole);
    }

    public static VaultVisibility fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Visibility is required");
        }
        return valueOf(value.trim().toUpperCase());
    }

    public static List<String> visibleDatabaseValues(RoleEnum role) {
        List<String> result = new ArrayList<>();
        for (VaultVisibility visibility : values()) {
            if (visibility.isVisibleTo(role)) result.add(visibility.name());
        }
        // Temporary read compatibility. New writes must never create MEMBERS.
        if (role != null && role.isOsuOrAbove()) result.add("MEMBERS");
        return List.copyOf(result);
    }
}
