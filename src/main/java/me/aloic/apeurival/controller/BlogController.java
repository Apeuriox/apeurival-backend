package me.aloic.apeurival.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.service.BlogService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/posts")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public Page<PostSummaryDTO> listPosts(
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String acceptLang) {

        log.info("[GET] handling getListPosts /api/posts in page {} with size of {}",page,size);
        String lang = acceptLang.toLowerCase().contains("en") ? "en" : "zh";
        return blogService.listPublishedPosts(tag, page, size, lang);
    }

    @GetMapping("/{slug}")
    public PostDetailDTO getPost(
            @PathVariable String slug,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String acceptLang) {
        log.info("[GET] handling getDetailedPost /api/posts/{}",slug);
        String lang = acceptLang.toLowerCase().contains("en") ? "en" : "zh";
        return blogService.getPostBySlug(slug, lang);
    }

    @PostMapping
    public PostDetailDTO createPost(@RequestBody PostRequest request, Authentication auth) {
        log.info("[POST] handling createPost /api/posts");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return blogService.createPost(request, userId);
    }

    @PutMapping("/{id}")
    public PostDetailDTO updatePost(@PathVariable Long id, @RequestBody PostRequest request, Authentication auth) {
        log.info("[PUT] handling updatePost /api/posts/{}",id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return blogService.updatePost(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable Long id, Authentication auth) {
        log.info("[DELETE] handling deletePost /api/posts/{}",id);
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        blogService.deletePost(id, userId);
    }
}
