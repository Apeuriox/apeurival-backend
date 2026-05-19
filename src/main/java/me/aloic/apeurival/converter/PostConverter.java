package me.aloic.apeurival.converter;

import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.entity.po.BlogPostPO;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PostConverter {

    private static final Parser mdParser = Parser.builder().build();
    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    private PostConverter() {}

    public static PostSummaryDTO toSummary(BlogPostPO po, String lang) {
        PostSummaryDTO dto = new PostSummaryDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(pickLang(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setExcerpt(pickLang(po.getExcerptZh(), po.getExcerptEn(), lang));
        dto.setTags(splitTags(po.getTags()));
        dto.setDate(po.getPublishedAt() != null ? po.getPublishedAt().toLocalDate() : null);
        dto.setCoverUrl(po.getCoverUrl());
        return dto;
    }

    public static PostDetailDTO toDetail(BlogPostPO po, String lang,
                                         BlogPostPO prev, BlogPostPO next) {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(pickLang(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setExcerpt(pickLang(po.getExcerptZh(), po.getExcerptEn(), lang));
        dto.setContentHtml(renderMarkdown(po.getContentMd()));
        dto.setTags(splitTags(po.getTags()));
        dto.setDate(po.getPublishedAt() != null ? po.getPublishedAt().toLocalDate() : null);
        dto.setCoverUrl(po.getCoverUrl());
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

    private static PostDetailDTO neighborDto(BlogPostPO po, String lang) {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(pickLang(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setCoverUrl(po.getCoverUrl());
        return dto;
    }

    private static String renderMarkdown(String md) {
        if (md == null || md.isBlank()) return "";
        Node document = mdParser.parse(md);
        return htmlRenderer.render(document);
    }
}
