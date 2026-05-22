package me.aloic.apeurival.converter;

import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.UserPO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PostConverter {

    private PostConverter() {}

    public static PostSummaryDTO toSummary(BlogPostPO po, String lang, UserPO author) {
        PostSummaryDTO dto = new PostSummaryDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(pickLang(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setExcerpt(pickLang(po.getExcerptZh(), po.getExcerptEn(), lang));
        dto.setTags(splitTags(po.getTags()));
        dto.setDate(po.getPublishedAt() != null ? po.getPublishedAt().toLocalDate() : null);
        dto.setCoverUrl(po.getCoverUrl());
        dto.setAuthor(authorBrief(author));
        return dto;
    }

    public static PostDetailDTO toDetail(BlogPostPO po, String lang,
                                         BlogPostPO prev, BlogPostPO next, UserPO author) {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(pickLang(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setExcerpt(pickLang(po.getExcerptZh(), po.getExcerptEn(), lang));
        dto.setContentMd(po.getContentMd());
        dto.setTags(splitTags(po.getTags()));
        dto.setDate(po.getPublishedAt() != null ? po.getPublishedAt().toLocalDate() : null);
        dto.setCoverUrl(po.getCoverUrl());
        dto.setAuthor(authorBrief(author));
        if (prev != null) dto.setPrev(neighborDto(prev, lang));
        if (next != null) dto.setNext(neighborDto(next, lang));
        return dto;
    }

    public static String pickLang(String zh, String en, String lang) {
        if ("en".equalsIgnoreCase(lang) && en != null && !en.isBlank()) {
            return en;
        }
        return zh != null ? zh : en;
    }

    public static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static PostDetailDTO.AuthorBrief authorBrief(UserPO user) {
        if (user == null) return null;
        PostDetailDTO.AuthorBrief ab = new PostDetailDTO.AuthorBrief();
        ab.setId(user.getId());
        ab.setDisplayName(user.getDisplayName());
        ab.setAvatarUrl(user.getAvatarUrl());
        ab.setProfileUrl(user.getProfileUrl());
        return ab;
    }

    private static PostDetailDTO neighborDto(BlogPostPO po, String lang) {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(pickLang(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setCoverUrl(po.getCoverUrl());
        return dto;
    }
}
