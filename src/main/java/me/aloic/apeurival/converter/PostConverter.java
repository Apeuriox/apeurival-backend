package me.aloic.apeurival.converter;

import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.UserPO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PostConverter
{
    //disable constructor
    private PostConverter() {}

    public static PostSummaryDTO setupPostSummaryDTO(BlogPostPO po, String lang, UserPO author)
    {
        PostSummaryDTO dto = new PostSummaryDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(selectLanguage(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setExcerpt(selectLanguage(po.getExcerptZh(), po.getExcerptEn(), lang));
        dto.setTags(splitTags(po.getTags()));
        dto.setDate(po.getPublishedAt() != null ? po.getPublishedAt().toLocalDate() : null);
        dto.setCoverUrl(po.getCoverUrl());
        dto.setCategory(po.getCategory());
        dto.setAuthor(transformUserPOtoAuthorBrief(author));
        return dto;
    }

    public static PostDetailDTO setupPostDetailDTO(BlogPostPO po, String lang,
                                                   BlogPostPO prev, BlogPostPO next, UserPO author)
    {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(selectLanguage(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setExcerpt(selectLanguage(po.getExcerptZh(), po.getExcerptEn(), lang));
        dto.setContentMd(po.getContentMd());
        dto.setTags(splitTags(po.getTags()));
        dto.setDate(po.getPublishedAt() != null ? po.getPublishedAt().toLocalDate() : null);
        dto.setCoverUrl(po.getCoverUrl());
        dto.setAuthor(transformUserPOtoAuthorBrief(author));
        dto.setCategory(po.getCategory());
        if (prev != null) dto.setPrev(setupPostNeighbors(prev, lang));
        if (next != null) dto.setNext(setupPostNeighbors(next, lang));
        return dto;
    }

    public static void setupBlogPostPO(BlogPostPO po, PostRequest req)
    {
        po.setSlug(req.getSlug());
        po.setTitleZh(req.getTitleZh());
        po.setTitleEn(req.getTitleEn());
        po.setExcerptZh(req.getExcerptZh());
        po.setExcerptEn(req.getExcerptEn());
        po.setContentMd(req.getContentMd());
        po.setCoverUrl(req.getCoverUrl());
        po.setTags(req.getTags());
        po.setCategory(req.getCategory());
        po.setStatus(req.getStatus());
    }

    public static String selectLanguage(String zh, String en, String lang)
    {
        if ("en".equalsIgnoreCase(lang) && en != null && !en.isBlank()) {
            return en;
        }
        return zh != null ? zh : en;
    }

    public static List<String> splitTags(String tags)
    {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static PostDetailDTO.AuthorBrief transformUserPOtoAuthorBrief(UserPO user)
    {
        if (user == null) return null;
        PostDetailDTO.AuthorBrief ab = new PostDetailDTO.AuthorBrief();
        ab.setId(user.getId());
        ab.setDisplayName(user.getDisplayName());
        ab.setAvatarUrl(user.getAvatarUrl());
        ab.setProfileUrl(user.getProfileUrl());
        return ab;
    }

    private static PostDetailDTO setupPostNeighbors(BlogPostPO po, String lang)
    {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(selectLanguage(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setCoverUrl(po.getCoverUrl());
        return dto;
    }
}
