package me.aloic.apeurival.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PostCategoryEnum {

    BLOG(1, "BLOG"),
    DOCUMENT(2, "DOCUMENT"),
    OTHER(3, "OTHER"),
    SHARING(4, "SHARING"),
    LOG(5, "LOG");

    private final int id;


    @EnumValue
    @JsonValue
    private final String category;


    @JsonCreator
    public static PostCategoryEnum fromString(String value) {
        if (value == null) return null;
        for (PostCategoryEnum e : values()) {
            if (e.category.equals(value)) return e;
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }
}
