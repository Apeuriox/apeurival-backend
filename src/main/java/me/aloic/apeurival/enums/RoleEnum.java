package me.aloic.apeurival.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Getter
@AllArgsConstructor
public enum RoleEnum {

    USER("USER", "User"),
    OSU("OSU", "User"),
    EDITOR("EDITOR", "Editor"),
    ADMIN("ADMIN", "Admin");

    private final String roleString;
    private final String roleShowcase;

    public static RoleEnum fromString(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        for (RoleEnum e : values()) {
            if (e.name().equalsIgnoreCase(s) || e.roleString.equalsIgnoreCase(s)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + s);
    }

    public static RoleEnum fromAuthority(String authority) {
        if (authority == null || !authority.startsWith("ROLE_")) {
            return null;
        }
        return fromString(authority.substring(5));
    }

    public GrantedAuthority toAuthority() {
        return new SimpleGrantedAuthority("ROLE_" + this.name());
    }

    public List<String> visibleVisibilities(boolean isOwner) {
        if (this == ADMIN || isOwner) return List.of("PUBLIC", "MEMBERS", "RESTRICTED", "PRIVATE");
        if (this == EDITOR)           return List.of("PUBLIC", "MEMBERS", "RESTRICTED");
        return List.of("PUBLIC", "MEMBERS");
    }

    public boolean canAssignExternalAuthor() {
        return this == ADMIN || this == EDITOR;
    }

    public boolean canManageSpecificGroup() {
        return this == ADMIN || this == EDITOR;
    }

    public boolean canCreateGroup() {
        return this == ADMIN;
    }

    public boolean isAdminOrEditor() {
        return this == ADMIN || this == EDITOR;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isAtLeastEditor() {
        return this == ADMIN || this == EDITOR;
    }
}
