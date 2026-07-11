package me.aloic.apeurival.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.mapper.WorkMapper;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.WorkPO;
import me.aloic.apeurival.service.ViewCountService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ViewCountFlushScheduler {

    private final ViewCountService viewCountService;
    private final BlogPostMapper blogPostMapper;
    private final WorkMapper workMapper;

    public ViewCountFlushScheduler(ViewCountService viewCountService,
                                   BlogPostMapper blogPostMapper,
                                   WorkMapper workMapper) {
        this.viewCountService = viewCountService;
        this.blogPostMapper = blogPostMapper;
        this.workMapper = workMapper;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void flushViewCounts() {
        log.info("Hourly view count flush started");
        int posts = flushPosts();
        int works = flushWorks();
        log.info("Hourly view count flush done: {} posts, {} works updated", posts, works);
    }

    private int flushPosts() {
        List<BlogPostPO> all = blogPostMapper.selectList(
                new QueryWrapper<BlogPostPO>().select("id", "view_count"));
        int updated = 0;
        for (BlogPostPO po : all) {
            long delta = viewCountService.getAndResetPost(po.getId());
            if (delta > 0) {
                long current = po.getViewCount() != null ? po.getViewCount() : 0;
                blogPostMapper.update(null,
                        new UpdateWrapper<BlogPostPO>()
                                .eq("id", po.getId())
                                .set("view_count", current + delta));
                updated++;
            }
        }
        return updated;
    }

    private int flushWorks() {
        List<WorkPO> all = workMapper.selectList(
                new QueryWrapper<WorkPO>().select("id", "view_count"));
        int updated = 0;
        for (WorkPO po : all) {
            long delta = viewCountService.getAndResetWork(po.getId());
            if (delta > 0) {
                long current = po.getViewCount() != null ? po.getViewCount() : 0;
                workMapper.update(null,
                        new UpdateWrapper<WorkPO>()
                                .eq("id", po.getId())
                                .set("view_count", current + delta));
                updated++;
            }
        }
        return updated;
    }
}
