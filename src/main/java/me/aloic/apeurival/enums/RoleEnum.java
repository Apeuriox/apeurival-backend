package me.aloic.apeurival.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Getter
@AllArgsConstructor
public enum RoleEnum {

    USER("USER", "User", 0),
    OSU("OSU", "User", 10),
    EDITOR("EDITOR", "Editor", 50),
    ADMIN("ADMIN", "Admin", 100);

    private static final int L_EDITOR = 50;
    private static final int L_ADMIN = 100;

    private final String roleString;
    private final String roleShowcase;
    private final int level;

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
        if (isOwner || this.level >= L_ADMIN) return List.of("PUBLIC", "MEMBERS", "RESTRICTED", "PRIVATE");
        if (this.level >= L_EDITOR)            return List.of("PUBLIC", "MEMBERS", "RESTRICTED");
        return List.of("PUBLIC", "MEMBERS");
    }

    public boolean canAssignExternalAuthor() {
        return this.level >= L_EDITOR;
    }

    public boolean canManageSpecificGroup() {
        return this.level >= L_EDITOR;
    }

    public boolean canCreateGroup() {
        return this.level >= L_ADMIN;
    }

    public boolean isAdmin() {
        return this.level >= L_ADMIN;
    }

    public boolean isAtLeastEditor() {
        return this.level >= L_EDITOR;
    }

    public boolean isOsuOrAbove() {
        return this.level >= OSU.level;
    }

    public boolean isAtLeast(RoleEnum min) {
        return this.level >= min.level;
    }
}
