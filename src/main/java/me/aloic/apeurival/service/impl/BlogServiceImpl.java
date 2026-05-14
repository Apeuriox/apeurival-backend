package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.service.BlogService;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BlogServiceImpl implements BlogService {

    private final BlogPostMapper blogPostMapper;
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    public BlogServiceImpl(BlogPostMapper blogPostMapper) {
        this.blogPostMapper = blogPostMapper;
    }

    @Override
    public Page<PostSummaryDTO> listPublishedPosts(String tag, int page, int size, String lang) {
        Page<BlogPostPO> poPage = new Page<>(page, size);
        QueryWrapper<BlogPostPO> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (tag != null && !tag.isBlank()) {
            wrapper.apply("FIND_IN_SET({0}, tags)", tag);
        }
        wrapper.orderByDesc("published_at");

        Page<BlogPostPO> result = blogPostMapper.selectPage(poPage, wrapper);

        List<PostSummaryDTO> dtoList = result.getRecords().stream()
                .map(po -> toSummary(po, lang))
                .toList();

        Page<PostSummaryDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    public PostDetailDTO getPostBySlug(String slug, String lang) {
        BlogPostPO post = blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>().eq("slug", slug));
        if (post == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + slug);
        }
        return toDetail(post, lang);
    }

    @Override
    public PostDetailDTO createPost(PostRequest request) {
        BlogPostPO po = new BlogPostPO();
        applyRequest(po, request);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        if (po.getStatus() != null && po.getStatus() == 1 && po.getPublishedAt() == null) {
            po.setPublishedAt(LocalDateTime.now());
        }
        blogPostMapper.insert(po);
        return toDetail(po, "zh");
    }

    @Override
    public PostDetailDTO updatePost(Long id, PostRequest request) {
        BlogPostPO po = blogPostMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: id=" + id);
        }
        applyRequest(po, request);
        po.setUpdatedAt(LocalDateTime.now());
        if (po.getStatus() != null && po.getStatus() == 1 && po.getPublishedAt() == null) {
            po.setPublishedAt(LocalDateTime.now());
        }
        blogPostMapper.updateById(po);
        return toDetail(po, "zh");
    }

    @Override
    public void deletePost(Long id) {
        BlogPostPO po = blogPostMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: id=" + id);
        }
        blogPostMapper.deleteById(id);
    }

    // ---- conversion helpers ----

    private PostSummaryDTO toSummary(BlogPostPO po, String lang) {
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

    private PostDetailDTO toDetail(BlogPostPO po, String lang) {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(po.getId());
        dto.setSlug(po.getSlug());
        dto.setTitle(pickLang(po.getTitleZh(), po.getTitleEn(), lang));
        dto.setExcerpt(pickLang(po.getExcerptZh(), po.getExcerptEn(), lang));
        dto.setContentHtml(renderMarkdown(po.getContentMd()));
        dto.setTags(splitTags(po.getTags()));
        dto.setDate(po.getPublishedAt() != null ? po.getPublishedAt().toLocalDate() : null);
        dto.setCoverUrl(po.getCoverUrl());

        if (po.getPublishedAt() != null) {
            dto.setPrev(fetchPrev(po, lang));
            dto.setNext(fetchNext(po, lang));
        }
        return dto;
    }

    private PostDetailDTO fetchPrev(BlogPostPO current, String lang) {
        BlogPostPO prev = blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>()
                        .eq("status", 1)
                        .lt("published_at", current.getPublishedAt())
                        .orderByDesc("published_at")
                        .last("LIMIT 1"));
        if (prev == null) return null;
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(prev.getId());
        dto.setSlug(prev.getSlug());
        dto.setTitle(pickLang(prev.getTitleZh(), prev.getTitleEn(), lang));
        dto.setCoverUrl(prev.getCoverUrl());
        return dto;
    }

    private PostDetailDTO fetchNext(BlogPostPO current, String lang) {
        BlogPostPO next = blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>()
                        .eq("status", 1)
                        .gt("published_at", current.getPublishedAt())
                        .orderByAsc("published_at")
                        .last("LIMIT 1"));
        if (next == null) return null;
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(next.getId());
        dto.setSlug(next.getSlug());
        dto.setTitle(pickLang(next.getTitleZh(), next.getTitleEn(), lang));
        dto.setCoverUrl(next.getCoverUrl());
        return dto;
    }

    private void applyRequest(BlogPostPO po, PostRequest req) {
        po.setSlug(req.getSlug());
        po.setTitleZh(req.getTitleZh());
        po.setTitleEn(req.getTitleEn());
        po.setExcerptZh(req.getExcerptZh());
        po.setExcerptEn(req.getExcerptEn());
        po.setContentMd(req.getContentMd());
        po.setCoverUrl(req.getCoverUrl());
        po.setTags(req.getTags());
        po.setStatus(req.getStatus());
    }

    private static String pickLang(String zh, String en, String lang) {
        if ("en".equalsIgnoreCase(lang) && en != null && !en.isBlank()) {
            return en;
        }
        return zh != null ? zh : en;
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String renderMarkdown(String md) {
        if (md == null || md.isBlank()) return "";
        Node document = markdownParser.parse(md);
        return htmlRenderer.render(document);
    }
}
