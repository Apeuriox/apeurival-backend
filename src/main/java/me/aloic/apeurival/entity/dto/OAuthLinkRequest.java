package me.aloic.apeurival.entity.dto;

import lombok.Data;

@Data
public class OAuthLinkRequest {
    private String code;
    private String state;
}
