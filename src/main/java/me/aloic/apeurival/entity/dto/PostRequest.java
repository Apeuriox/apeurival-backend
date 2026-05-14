package me.aloic.apeurival.entity.dto;

import lombok.Data;

@Data
public class PostRequest {
    private String slug;
    private String titleZh;
    private String titleEn;
    private String excerptZh;
    private String excerptEn;
    private String contentMd;
    private String coverUrl;
    private String tags;
    private Integer status;
}
