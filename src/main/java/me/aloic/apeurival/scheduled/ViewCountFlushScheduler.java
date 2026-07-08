package me.aloic.apeurival.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
        List<BlogPostPO> all = blogPostMapper.selectList(new QueryWrapper<BlogPostPO>().select("id"));
        int updated = 0;
        for (BlogPostPO po : all) {
            long delta = viewCountService.getAndResetPost(po.getId());
            if (delta > 0) {
                BlogPostPO update = new BlogPostPO();
                update.setId(po.getId());
                update.setViewCount(po.getViewCount() != null ? po.getViewCount() + delta : delta);
                blogPostMapper.updateById(update);
                updated++;
            }
        }
        return updated;
    }

    private int flushWorks() {
        List<WorkPO> all = workMapper.selectList(new QueryWrapper<WorkPO>().select("id"));
        int updated = 0;
        for (WorkPO po : all) {
            long delta = viewCountService.getAndResetWork(po.getId());
            if (delta > 0) {
                WorkPO update = new WorkPO();
                update.setId(po.getId());
                update.setViewCount(po.getViewCount() != null ? po.getViewCount() + delta : delta);
                workMapper.updateById(update);
                updated++;
            }
        }
        return updated;
    }
}
