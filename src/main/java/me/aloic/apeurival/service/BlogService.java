package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;
import me.aloic.apeurival.enums.PostCategoryEnum;

public interface BlogService {

    Page<PostSummaryDTO> listPublishedPosts(String tag, PostCategoryEnum category, int page, int size, String lang);

    PostDetailDTO getPostBySlug(String slug, String lang);

    PostDetailDTO createPost(PostRequest request, Long authorId);

    PostDetailDTO updatePost(Long id, PostRequest request, Long userId);

    void deletePost(Long id, Long userId);
}
