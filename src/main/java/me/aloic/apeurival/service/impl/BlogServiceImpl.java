package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.converter.PostConverter;
import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@Service
public class BlogServiceImpl implements BlogService {

    private final BlogPostMapper blogPostMapper;
    private final UserMapper userMapper;

    public BlogServiceImpl(BlogPostMapper blogPostMapper, UserMapper userMapper) {
        this.blogPostMapper = blogPostMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Page<PostSummaryDTO> listPublishedPosts(String tag, int page, int size, String lang) {
        Page<BlogPostPO> blogPostPage = new Page<>(page, size);
        QueryWrapper<BlogPostPO> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (tag != null && !tag.isBlank()) {
            wrapper.apply("FIND_IN_SET({0}, tags)", tag);
        }
        wrapper.orderByDesc("published_at");

        Page<BlogPostPO> result = blogPostMapper.selectPage(blogPostPage, wrapper);

        Page<PostSummaryDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(po -> PostConverter.toSummary(po, lang, userMapper.selectById(po.getAuthorId())))
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
                fetchPrev(post), fetchNext(post),
                userMapper.selectById(post.getAuthorId()));
    }

    @Override
    public PostDetailDTO createPost(PostRequest request, Long authorId) {
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            BlogPostPO existing = blogPostMapper.selectOne(
                    new QueryWrapper<BlogPostPO>().eq("slug", request.getSlug()));
            if (existing != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists: " + request.getSlug());
            }
        }
        BlogPostPO po = new BlogPostPO();
        applyRequest(po, request);
        po.setAuthorId(authorId);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        if (po.getStatus() != null && po.getStatus() == 1 && po.getPublishedAt() == null) {
            po.setPublishedAt(LocalDateTime.now());
        }
        blogPostMapper.insert(po);
        log.info("successfully create new post");
        UserPO author = userMapper.selectById(authorId);
        return PostConverter.toDetail(po, "zh", null, null, author);
    }

    @Override
    public PostDetailDTO updatePost(Long id, PostRequest request, Long userId) {
        BlogPostPO po = blogPostMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: id=" + id);
        }
        checkOwnership(po, userId);
        applyRequest(po, request);
        po.setUpdatedAt(LocalDateTime.now());
        if (po.getStatus() != null && po.getStatus() == 1 && po.getPublishedAt() == null) {
            po.setPublishedAt(LocalDateTime.now());
        }
        blogPostMapper.updateById(po);
        log.info("successfully updated post {}",id);
        UserPO author = userMapper.selectById(po.getAuthorId());
        return PostConverter.toDetail(po, "zh", null, null, author);
    }

    @Override
    public void deletePost(Long id, Long userId) {
        BlogPostPO po = blogPostMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: id=" + id);
        }
        checkOwnership(po, userId);
        log.info("successfully deleted post {}",id);
        blogPostMapper.deleteById(id);
    }

    private void checkOwnership(BlogPostPO po, Long userId) {
        if (po.getAuthorId() != null && po.getAuthorId().equals(userId)) return;
        UserPO caller = userMapper.selectById(userId);
        if (caller != null && "ADMIN".equals(caller.getRole())) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own posts");
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
