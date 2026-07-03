package me.aloic.apeurival.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleGroupEnum {

    MEMBER("MEMBER"),
    MANAGER("MANAGER");

    private final String roleString;

    public static RoleGroupEnum fromString(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        for (RoleGroupEnum e : values()) {
            if (e.name().equalsIgnoreCase(s) || e.roleString.equalsIgnoreCase(s)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown group role: " + s);
    }
}
