package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.converter.PostConverter;
import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.enums.EntityTypeEnum;
import me.aloic.apeurival.enums.PostCategoryEnum;
import me.aloic.apeurival.enums.RoleEnum;
import me.aloic.apeurival.service.BlogService;
import me.aloic.apeurival.service.OperationLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class BlogServiceImpl implements BlogService {

    private final BlogPostMapper blogPostMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public BlogServiceImpl(BlogPostMapper blogPostMapper, UserMapper userMapper,
                           OperationLogService operationLogService,
                           ObjectMapper objectMapper) {
        this.blogPostMapper = blogPostMapper;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Page<PostSummaryDTO> listPublishedPosts(String tag, PostCategoryEnum category, Long authorId,
                                                    String sort, int page, int size, String lang) {
        Page<BlogPostPO> blogPostPage = new Page<>(page, size);
        QueryWrapper<BlogPostPO> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (tag != null && !tag.isBlank()) {
            wrapper.apply("FIND_IN_SET({0}, tags)", tag);
        }
        if (category != null) {
            wrapper.eq("category", category);
        }
        if (authorId != null) {
            wrapper.eq("author_id", authorId);
        }
        applySortToQueryWrapper(wrapper, sort);

        Page<BlogPostPO> result = blogPostMapper.selectPage(blogPostPage, wrapper);

        Page<PostSummaryDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(po -> PostConverter.setupPostSummaryDTO(po, lang, userMapper.selectById(po.getAuthorId())))
                .toList());
        log.info("Final post size: {}",dtoPage.getTotal());
        return dtoPage;
    }

    @Override
    public PostDetailDTO getPostBySlug(String slug, String lang) {
        BlogPostPO post = blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>().eq("slug", slug));
        if (post == null) {
            log.warn("Cant get target post, requested slug was {}",slug);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + slug);
        }
        return PostConverter.setupPostDetailDTO(post, lang,
                fetchPrevPost(post), fetchNextPost(post),
                userMapper.selectById(post.getAuthorId()));
    }

    @Override
    public PostDetailDTO createPost(PostRequest request, Long authorId) {
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            log.info("Creating post with certain slug...target slug was {}", request.getSlug());
            BlogPostPO existing = blogPostMapper.selectOne(
                    new QueryWrapper<BlogPostPO>().eq("slug", request.getSlug()));
            if (existing != null) {
                log.warn("Target slug exists, skipping...");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists: " + request.getSlug());
            }
        }
        else {
            request.setSlug(String.valueOf(UUID.randomUUID()));
        }
        BlogPostPO po = new BlogPostPO();
        PostConverter.setupBlogPostPO(po, request);
        po.setAuthorId(authorId);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        if (po.getStatus() != null && po.getStatus() == 1 && po.getPublishedAt() == null) {
            po.setPublishedAt(LocalDateTime.now());
        }
        blogPostMapper.insert(po);
        log.info("successfully create new post with slug of {}",request.getSlug());
        operationLogService.logCreate(EntityTypeEnum.POST.getCode(), po.getId(), authorId, po);
        UserPO author = userMapper.selectById(authorId);
        return PostConverter.setupPostDetailDTO(po, "zh", null, null, author);
    }

    @Override
    public PostDetailDTO updatePost(Long id, PostRequest request, Long userId) {
        BlogPostPO po = blogPostMapper.selectById(id);
        if (po == null) {
            log.warn("No such post {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: id=" + id);
        }
        checkOwnership(po, userId);
        BlogPostPO oldPo = clonePo(po);
        PostConverter.setupBlogPostPO(po, request);
        po.setUpdatedAt(LocalDateTime.now());
        if (po.getStatus() != null && po.getStatus() == 1 && po.getPublishedAt() == null) {
            po.setPublishedAt(LocalDateTime.now());
        }
        blogPostMapper.updateById(po);
        log.info("successfully updated post {}",id);
        operationLogService.logUpdate(EntityTypeEnum.POST.getCode(), id, userId, po, oldPo);
        UserPO author = userMapper.selectById(po.getAuthorId());
        return PostConverter.setupPostDetailDTO(po, "zh", null, null, author);
    }

    @Override
    public void deletePost(Long id, Long userId) {
        BlogPostPO po = blogPostMapper.selectById(id);
        if (po == null) {
            log.warn("No such post to delete {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: id=" + id);
        }
        checkOwnership(po, userId);
        log.info("successfully deleted post {}",id);
        operationLogService.logDelete(EntityTypeEnum.POST.getCode(), id, userId, po);
        blogPostMapper.deleteById(id);
    }

    private void checkOwnership(BlogPostPO po, Long userId) {
        if (po.getAuthorId() != null && po.getAuthorId().equals(userId)) return;
        UserPO caller = userMapper.selectById(userId);
        if (caller != null && RoleEnum.ADMIN == RoleEnum.fromString(caller.getRole())) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own posts");
    }

    private BlogPostPO fetchPrevPost(BlogPostPO current) {
        return blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>()
                        .eq("status", 1)
                        .lt("published_at", current.getPublishedAt())
                        .orderByDesc("published_at")
                        .last("LIMIT 1"));
    }

    private BlogPostPO fetchNextPost(BlogPostPO current) {
        return blogPostMapper.selectOne(
                new QueryWrapper<BlogPostPO>()
                        .eq("status", 1)
                        .gt("published_at", current.getPublishedAt())
                        .orderByAsc("published_at")
                        .last("LIMIT 1"));
    }

    private void applySortToQueryWrapper(QueryWrapper<BlogPostPO> wrapper, String sort) {
        if (sort == null || "newest".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc("published_at");
        } else if ("oldest".equalsIgnoreCase(sort)) {
            wrapper.orderByAsc("published_at");
        } else if ("title_asc".equalsIgnoreCase(sort)) {
            wrapper.orderByAsc("title_zh");
        } else if ("title_desc".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc("title_zh");
        } else {
            wrapper.orderByDesc("published_at");
        }
    }

    private BlogPostPO clonePo(BlogPostPO po) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(po), BlogPostPO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone entity", e);
        }
    }
}
