package me.aloic.apeurival.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.annotation.RequireAuth;
import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.service.BlogService;
import org.springframework.web.bind.annotation.*;

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
        String lang = acceptLang.toLowerCase().contains("en") ? "en" : "zh";
        return blogService.listPublishedPosts(tag, page, size, lang);
    }

    @GetMapping("/{slug}")
    public PostDetailDTO getPost(
            @PathVariable String slug,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String acceptLang) {
        String lang = acceptLang.toLowerCase().contains("en") ? "en" : "zh";
        return blogService.getPostBySlug(slug, lang);
    }

    @PostMapping
    @RequireAuth
    public PostDetailDTO createPost(@RequestBody PostRequest request) {
        return blogService.createPost(request);
    }

    @PutMapping("/{id}")
    @RequireAuth
    public PostDetailDTO updatePost(@PathVariable Long id, @RequestBody PostRequest request) {
        return blogService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    public void deletePost(@PathVariable Long id) {
        blogService.deletePost(id);
    }
}
