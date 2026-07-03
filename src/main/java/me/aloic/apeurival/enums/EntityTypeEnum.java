package me.aloic.apeurival.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityTypeEnum {

    POST("POST", me.aloic.apeurival.entity.po.BlogPostPO.class),
    WORK("WORK", me.aloic.apeurival.entity.po.WorkPO.class),
    VAULT_ITEM("VAULT_ITEM", me.aloic.apeurival.entity.po.VaultItemPO.class);

    private final String code;
    private final Class<?> poClass;

    public static EntityTypeEnum fromCode(String code) {
        if (code == null) return null;
        for (EntityTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        throw new IllegalArgumentException("Unknown entity type: " + code);
    }
}
