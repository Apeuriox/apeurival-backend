package me.aloic.apeurival.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.config.UploadPathConfig;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.WorkImageMapper;
import me.aloic.apeurival.entity.mapper.WorkMapper;
import me.aloic.apeurival.entity.mapper.WorkMomentMapper;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.WorkImagePO;
import me.aloic.apeurival.entity.po.WorkMomentPO;
import me.aloic.apeurival.entity.po.WorkPO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class FileCleanupScheduler {

    private static final Pattern MD_IMAGE = Pattern.compile("!\\[.*?]\\(/uploads/(?:images/)?([^)]+)\\)");

    private final Path imagesDir;
    private final WorkMapper workMapper;
    private final WorkImageMapper workImageMapper;
    private final WorkMomentMapper workMomentMapper;
    private final BlogPostMapper blogPostMapper;
    private final UserMapper userMapper;

    public FileCleanupScheduler(UploadPathConfig uploadPathConfig,
                                WorkMapper workMapper,
                                WorkImageMapper workImageMapper,
                                WorkMomentMapper workMomentMapper,
                                BlogPostMapper blogPostMapper,
                                UserMapper userMapper) {
        this.workMapper = workMapper;
        this.workImageMapper = workImageMapper;
        this.workMomentMapper = workMomentMapper;
        this.blogPostMapper = blogPostMapper;
        this.userMapper = userMapper;
        this.imagesDir = Paths.get(uploadPathConfig.resolve(), "images").toAbsolutePath();
    }

    @Scheduled(cron = "0 0 3 * * SUN")
    public void scheduledCleanup() {
        int removed = cleanOrphanFiles();
        if (removed > 0) {
            log.info("Weekly orphan cleanup: {} files removed", removed);
        }
    }

    public List<String> listOrphanFiles() {
        if (Files.notExists(imagesDir)) return List.of();
        Set<String> referenced = collectAllReferencedFilenames();
        try (var files = Files.list(imagesDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(f -> !referenced.contains(f.getFileName().toString()))
                    .map(p -> p.getFileName().toString())
                    .toList();
        } catch (IOException e) {
            log.error("Orphan scan failed", e);
            return List.of();
        }
    }

    public int cleanOrphanFiles() {
        if (Files.notExists(imagesDir)) return 0;
        Set<String> referenced = collectAllReferencedFilenames();
        int removed = 0;
        try (var files = Files.list(imagesDir)) {
            List<Path> orphans = files
                    .filter(Files::isRegularFile)
                    .filter(f -> !referenced.contains(f.getFileName().toString()))
                    .toList();
            for (Path orphan : orphans) {
                Files.delete(orphan);
                log.info("Cleaned orphan: {}", orphan.getFileName());
                removed++;
            }
        } catch (IOException e) {
            log.error("Orphan scan failed", e);
        }
        return removed;
    }

    private Set<String> collectAllReferencedFilenames() {
        Set<String> refs = new HashSet<>();

        // works.cover_url
        List<WorkPO> works = workMapper.selectList(
                new QueryWrapper<WorkPO>().select("cover_url").isNotNull("cover_url"));
        for (WorkPO w : works) extractFilename(w.getCoverUrl(), refs);

        // work_images.image_url
        List<WorkImagePO> images = workImageMapper.selectList(
                new QueryWrapper<WorkImagePO>().select("image_url").isNotNull("image_url"));
        for (WorkImagePO wi : images) extractFilename(wi.getImageUrl(), refs);

        // work_moments.image_url
        List<WorkMomentPO> moments = workMomentMapper.selectList(
                new QueryWrapper<WorkMomentPO>().select("image_url").isNotNull("image_url"));
        for (WorkMomentPO m : moments) extractFilename(m.getImageUrl(), refs);

        // posts.cover_url
        List<BlogPostPO> posts = blogPostMapper.selectList(
                new QueryWrapper<BlogPostPO>().select("cover_url").isNotNull("cover_url"));
        for (BlogPostPO p : posts) extractFilename(p.getCoverUrl(), refs);

        // posts.content_md — extract markdown images
        List<BlogPostPO> postContents = blogPostMapper.selectList(
                new QueryWrapper<BlogPostPO>().select("content_md").isNotNull("content_md"));
        for (BlogPostPO p : postContents) {
            Matcher m = MD_IMAGE.matcher(p.getContentMd());
            while (m.find()) refs.add(m.group(1));
        }

        // users.avatar_url
        List<UserPO> users = userMapper.selectList(
                new QueryWrapper<UserPO>().select("avatar_url").isNotNull("avatar_url"));
        for (UserPO u : users) extractFilename(u.getAvatarUrl(), refs);

        return refs;
    }

    private static void extractFilename(String url, Set<String> out) {
        if (url == null || url.isBlank()) return;
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (!name.isBlank()) out.add(name);
    }
}
