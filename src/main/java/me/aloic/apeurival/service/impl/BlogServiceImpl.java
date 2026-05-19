package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.converter.PostConverter;
import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class BlogServiceImpl implements BlogService {

    private final BlogPostMapper blogPostMapper;

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

        Page<PostSummaryDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(po -> PostConverter.toSummary(po, lang))
                .toList());
        return dtoPage;
    }

    @Override
    public PostDetailDTO getPostBySlug(String slug, String lang) {
        BlogPostPO post = blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>().eq("slug", slug));
        if (post == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + slug);
        }
        return PostConverter.toDetail(post, lang,
                fetchPrev(post), fetchNext(post));
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
        return PostConverter.toDetail(po, "zh", null, null);
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
        return PostConverter.toDetail(po, "zh", null, null);
    }

    @Override
    public void deletePost(Long id) {
        BlogPostPO po = blogPostMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: id=" + id);
        }
        blogPostMapper.deleteById(id);
    }

    private BlogPostPO fetchPrev(BlogPostPO current) {
        return blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>()
                        .eq("status", 1)
                        .lt("published_at", current.getPublishedAt())
                        .orderByDesc("published_at")
                        .last("LIMIT 1"));
    }

    private BlogPostPO fetchNext(BlogPostPO current) {
        return blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>()
                        .eq("status", 1)
                        .gt("published_at", current.getPublishedAt())
                        .orderByAsc("published_at")
                        .last("LIMIT 1"));
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
}
