package me.aloic.apeurival.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.config.UploadPathConfig;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.mapper.OperationLogMapper;
import me.aloic.apeurival.entity.mapper.UploadMetaMapper;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.VaultItemMapper;
import me.aloic.apeurival.entity.mapper.WorkImageMapper;
import me.aloic.apeurival.entity.mapper.WorkMapper;
import me.aloic.apeurival.entity.mapper.WorkMomentMapper;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.OperationLogPO;
import me.aloic.apeurival.entity.po.UploadMetaPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.VaultItemPO;
import me.aloic.apeurival.entity.po.WorkImagePO;
import me.aloic.apeurival.entity.po.WorkMomentPO;
import me.aloic.apeurival.entity.po.WorkPO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class FileCleanupScheduler {

    private static final Pattern MD_IMAGE = Pattern.compile("!\\[.*?]\\(/uploads/(?:images/)?([^)]+)\\)");
    private static final int UNBOUND_TTL_HOURS = 24;

    private final Path imagesDir;
    private final UploadMetaMapper uploadMetaMapper;
    private final WorkMapper workMapper;
    private final WorkImageMapper workImageMapper;
    private final WorkMomentMapper workMomentMapper;
    private final BlogPostMapper blogPostMapper;
    private final VaultItemMapper vaultItemMapper;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    public FileCleanupScheduler(UploadPathConfig uploadPathConfig,
                                UploadMetaMapper uploadMetaMapper,
                                WorkMapper workMapper,
                                WorkImageMapper workImageMapper,
                                WorkMomentMapper workMomentMapper,
                                BlogPostMapper blogPostMapper,
                                VaultItemMapper vaultItemMapper,
                                OperationLogMapper operationLogMapper,
                                UserMapper userMapper) {
        this.uploadMetaMapper = uploadMetaMapper;
        this.workMapper = workMapper;
        this.workImageMapper = workImageMapper;
        this.workMomentMapper = workMomentMapper;
        this.blogPostMapper = blogPostMapper;
        this.vaultItemMapper = vaultItemMapper;
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
        this.imagesDir = Paths.get(uploadPathConfig.resolve(), "images").toAbsolutePath();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledCleanup() {
        log.info("Hourly upload cleanup started");
        int bound = bindReferencedFiles();
        int removed = cleanExpiredUnboundFiles();
        log.info("Hourly cleanup done: {} files bound, {} expired files removed", bound, removed);
    }

    public int bindReferencedFiles() {
        Set<String> referenced = collectAllReferencedFilenames();
        if (referenced.isEmpty()) return 0;
        List<UploadMetaPO> unbound = uploadMetaMapper.selectList(
                new QueryWrapper<UploadMetaPO>().eq("bound", false));
        int count = 0;
        for (UploadMetaPO meta : unbound) {
            if (referenced.contains(meta.getFilename())) {
                meta.setBound(true);
                uploadMetaMapper.updateById(meta);
                count++;
            }
        }
        if (count > 0) {
            log.info("Bound {} uploaded files", count);
        }
        return count;
    }

    public int cleanExpiredUnboundFiles() {
        if (Files.notExists(imagesDir)) return 0;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(UNBOUND_TTL_HOURS);
        List<UploadMetaPO> expired = uploadMetaMapper.selectList(
                new QueryWrapper<UploadMetaPO>()
                        .eq("bound", false)
                        .le("created_at", cutoff));
        int removed = 0;
        for (UploadMetaPO meta : expired) {
            Path file = imagesDir.resolve(meta.getFilename());
            try {
                if (Files.exists(file)) {
                    Files.delete(file);
                }
                uploadMetaMapper.deleteById(meta.getId());
                log.info("Cleaned expired unbound file: {}", meta.getFilename());
                removed++;
            } catch (IOException e) {
                log.error("Failed to delete file {}", meta.getFilename(), e);
            }
        }
        return removed;
    }

    public List<UploadMetaPO> listUnboundFiles() {
        return uploadMetaMapper.selectList(
                new QueryWrapper<UploadMetaPO>()
                        .eq("bound", false)
                        .orderByAsc("created_at"));
    }

    private Set<String> collectAllReferencedFilenames() {
        Set<String> refs = new HashSet<>();

        List<WorkPO> works = workMapper.selectList(
                new QueryWrapper<WorkPO>().select("cover_url", "content_md")
                        .and(w -> w.isNotNull("cover_url").or().isNotNull("content_md")));
        for (WorkPO w : works) {
            extractFilename(w.getCoverUrl(), refs);
            extractMarkdownImages(w.getContentMd(), refs);
        }

        List<WorkImagePO> images = workImageMapper.selectList(
                new QueryWrapper<WorkImagePO>().select("image_url").isNotNull("image_url"));
        for (WorkImagePO wi : images) extractFilename(wi.getImageUrl(), refs);

        List<WorkMomentPO> moments = workMomentMapper.selectList(
                new QueryWrapper<WorkMomentPO>().select("image_url").isNotNull("image_url"));
        for (WorkMomentPO m : moments) extractFilename(m.getImageUrl(), refs);

        List<BlogPostPO> posts = blogPostMapper.selectList(
                new QueryWrapper<BlogPostPO>().select("cover_url", "content_md")
                        .and(w -> w.isNotNull("cover_url").or().isNotNull("content_md")));
        for (BlogPostPO p : posts) {
            extractFilename(p.getCoverUrl(), refs);
            extractMarkdownImages(p.getContentMd(), refs);
        }

        List<VaultItemPO> vaultItems = vaultItemMapper.selectList(
                new QueryWrapper<VaultItemPO>().select("image_url").isNotNull("image_url"));
        for (VaultItemPO v : vaultItems) extractFilename(v.getImageUrl(), refs);

        List<UserPO> users = userMapper.selectList(
                new QueryWrapper<UserPO>().select("avatar_url").isNotNull("avatar_url"));
        for (UserPO u : users) extractFilename(u.getAvatarUrl(), refs);

        List<OperationLogPO> logs = operationLogMapper.selectList(
                new QueryWrapper<OperationLogPO>().select("entity_snapshot", "previous_snapshot")
                        .and(w -> w.isNotNull("entity_snapshot").or().isNotNull("previous_snapshot")));
        for (OperationLogPO l : logs) {
            extractJsonImageFields(l.getEntitySnapshot(), refs);
            extractJsonImageFields(l.getPreviousSnapshot(), refs);
        }

        return refs;
    }

    private static void extractFilename(String url, Set<String> out) {
        if (url == null || url.isBlank()) return;
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (!name.isBlank()) out.add(name);
    }

    private void extractMarkdownImages(String contentMd, Set<String> out) {
        if (contentMd == null || contentMd.isBlank()) return;
        Matcher m = MD_IMAGE.matcher(contentMd);
        while (m.find()) out.add(m.group(1));
    }

    private void extractJsonImageFields(String json, Set<String> out) {
        if (json == null || json.isBlank()) return;
        extractFromJsonField(json, "coverUrl", out);
        extractFromJsonField(json, "imageUrl", out);
        extractFromJsonField(json, "avatarUrl", out);
        extractJsonMdField(json, "contentMd", out);
    }

    private void extractFromJsonField(String json, String field, Set<String> out) {
        java.util.regex.Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        while (m.find()) {
            String val = m.group(1);
            if (!val.isBlank()) extractFilename(val, out);
        }
    }

    private void extractJsonMdField(String json, String field, Set<String> out) {
        int start = json.indexOf("\"" + field + "\"");
        if (start < 0) return;
        int colon = json.indexOf(':', start);
        if (colon < 0) return;
        int quote = json.indexOf('"', colon);
        if (quote < 0) return;
        int endQuote = json.indexOf('"', quote + 1);
        if (endQuote < 0) return;
        String contentMd = json.substring(quote + 1, endQuote);
        extractMarkdownImages(contentMd, out);
    }
}
