package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.PostDetailDTO;
import me.aloic.apeurival.entity.dto.PostRequest;
import me.aloic.apeurival.entity.dto.PostSummaryDTO;

public interface BlogService {

    Page<PostSummaryDTO> listPublishedPosts(String tag, int page, int size, String lang);

    PostDetailDTO getPostBySlug(String slug, String lang);

    PostDetailDTO createPost(PostRequest request);

    PostDetailDTO updatePost(Long id, PostRequest request);

    void deletePost(Long id);
}
