package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.converter.WorkConverter;
import me.aloic.apeurival.entity.dto.WorkDetailDTO;
import me.aloic.apeurival.entity.dto.WorkRequest;
import me.aloic.apeurival.entity.dto.WorkSummaryDTO;
import me.aloic.apeurival.entity.mapper.CodeWorkMapper;
import me.aloic.apeurival.entity.mapper.ImageWorkMapper;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.VideoWorkMapper;
import me.aloic.apeurival.entity.mapper.WorkImageMapper;
import me.aloic.apeurival.entity.mapper.WorkMomentMapper;
import me.aloic.apeurival.entity.mapper.WorkMapper;
import me.aloic.apeurival.entity.po.CodeWorkPO;
import me.aloic.apeurival.entity.po.ImageWorkPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.VideoWorkPO;
import me.aloic.apeurival.entity.po.WorkImagePO;
import me.aloic.apeurival.entity.po.WorkMomentPO;
import me.aloic.apeurival.entity.po.WorkPO;
import me.aloic.apeurival.service.WorkService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkServiceImpl implements WorkService {

    private final WorkMapper workMapper;
    private final CodeWorkMapper codeWorkMapper;
    private final ImageWorkMapper imageWorkMapper;
    private final VideoWorkMapper videoWorkMapper;
    private final WorkImageMapper workImageMapper;
    private final WorkMomentMapper workMomentMapper;
    private final UserMapper userMapper;

    public WorkServiceImpl(WorkMapper workMapper,
                           CodeWorkMapper codeWorkMapper,
                           ImageWorkMapper imageWorkMapper,
                           VideoWorkMapper videoWorkMapper,
                           WorkImageMapper workImageMapper,
                           WorkMomentMapper workMomentMapper,
                           UserMapper userMapper) {
        this.workMapper = workMapper;
        this.codeWorkMapper = codeWorkMapper;
        this.imageWorkMapper = imageWorkMapper;
        this.videoWorkMapper = videoWorkMapper;
        this.workImageMapper = workImageMapper;
        this.workMomentMapper = workMomentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Page<WorkSummaryDTO> listPublishedWorks(String type, int page, int size) {
        Page<WorkPO> poPage = new Page<>(page, size);
        QueryWrapper<WorkPO> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (type != null && !type.isBlank()) {
            wrapper.eq("type", type.toUpperCase());
        }
        wrapper.orderByDesc("created_at");

        Page<WorkPO> result = workMapper.selectPage(poPage, wrapper);
        Page<WorkSummaryDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(po -> WorkConverter.toSummary(po, userMapper.selectById(po.getAuthorId())))
                .toList());
        return dtoPage;
    }

    @Override
    public WorkDetailDTO getWork(Long id) {
        WorkPO po = workMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work not found");
        }
        return buildDetail(po);
    }

    @Override
    @Transactional
    public WorkDetailDTO createWork(WorkRequest req, Long authorId) {
        WorkPO po = new WorkPO();
        applyRequest(po, req);
        po.setAuthorId(authorId);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        workMapper.insert(po);
        insertSubRecord(po.getId(), req);
        return buildDetail(po);
    }

    @Override
    @Transactional
    public WorkDetailDTO updateWork(Long id, WorkRequest req, Long authorId) {
        WorkPO po = workMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work not found");
        }
        applyRequest(po, req);
        po.setAuthorId(authorId);
        po.setUpdatedAt(LocalDateTime.now());
        workMapper.updateById(po);
        deleteSubRecord(id, po.getType());
        insertSubRecord(id, req);
        return buildDetail(po);
    }

    @Override
    @Transactional
    public void deleteWork(Long id) {
        WorkPO po = workMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work not found");
        }
        deleteSubRecord(id, po.getType());
        workMapper.deleteById(id);
    }

    private WorkDetailDTO buildDetail(WorkPO po) {
        UserPO author = po.getAuthorId() != null ? userMapper.selectById(po.getAuthorId()) : null;
        CodeWorkPO code = null;
        ImageWorkPO image = null;
        VideoWorkPO video = null;
        List<WorkImagePO> images = null;
        List<WorkMomentPO> moments = null;
        switch (po.getType()) {
            case "CODE" -> code = codeWorkMapper.selectById(po.getId());
            case "IMAGE" -> {
                image = imageWorkMapper.selectById(po.getId());
                images = workImageMapper.selectList(
                        new QueryWrapper<WorkImagePO>().eq("work_id", po.getId())
                                .orderByAsc("sort_order"));
            }
            case "VIDEO" -> video = videoWorkMapper.selectById(po.getId());
        }
        moments = workMomentMapper.selectList(
                new QueryWrapper<WorkMomentPO>().eq("work_id", po.getId())
                        .orderByAsc("sort_order"));
        return WorkConverter.toDetail(po, author, code, image, video, images, moments);
    }

    private void insertSubRecord(Long workId, WorkRequest req) {
        String type = req.getType() != null ? req.getType().toUpperCase() : null;
        if ("CODE".equals(type)) {
            CodeWorkPO sub = new CodeWorkPO();
            sub.setWorkId(workId);
            sub.setRepoUrl(req.getRepoUrl());
            sub.setLanguages(req.getLanguages());
            codeWorkMapper.insert(sub);
        } else if ("IMAGE".equals(type)) {
            ImageWorkPO sub = new ImageWorkPO();
            sub.setWorkId(workId);
            sub.setWidth(req.getWidth());
            sub.setHeight(req.getHeight());
            sub.setFormat(req.getFormat());
            imageWorkMapper.insert(sub);

            if (req.getImages() != null) {
                int order = 0;
                for (WorkRequest.ImageRequest img : req.getImages()) {
                    WorkImagePO wi = new WorkImagePO();
                    wi.setWorkId(workId);
                    wi.setImageUrl(img.getImageUrl());
                    wi.setLabel(img.getLabel());
                    wi.setSortOrder(order++);
                    workImageMapper.insert(wi);
                }
            }
        } else if ("VIDEO".equals(type)) {
            VideoWorkPO sub = new VideoWorkPO();
            sub.setWorkId(workId);
            sub.setBvid(req.getBvid());
            sub.setPlatform(req.getPlatform() != null ? req.getPlatform() : "bilibili");
            videoWorkMapper.insert(sub);
        }

        if (req.getMoments() != null) {
            int order = 0;
            for (WorkRequest.MomentRequest m : req.getMoments()) {
                WorkMomentPO wm = new WorkMomentPO();
                wm.setWorkId(workId);
                wm.setImageUrl(m.getImageUrl());
                wm.setContent(m.getContent());
                wm.setMomentTime(m.getMomentTime());
                wm.setSortOrder(order++);
                workMomentMapper.insert(wm);
            }
        }
    }

    private void deleteSubRecord(Long workId, String type) {
        workMomentMapper.delete(new QueryWrapper<WorkMomentPO>().eq("work_id", workId));
        if ("CODE".equals(type)) {
            codeWorkMapper.deleteById(workId);
        } else if ("IMAGE".equals(type)) {
            workImageMapper.delete(new QueryWrapper<WorkImagePO>().eq("work_id", workId));
            imageWorkMapper.deleteById(workId);
        } else if ("VIDEO".equals(type)) {
            videoWorkMapper.deleteById(workId);
        }
    }

    private void applyRequest(WorkPO po, WorkRequest req) {
        if (req.getTitle() != null) po.setTitle(req.getTitle());
        if (req.getDescription() != null) po.setDescription(req.getDescription());
        if (req.getType() != null) po.setType(req.getType().toUpperCase());
        if (req.getCoverUrl() != null) po.setCoverUrl(req.getCoverUrl());
        if (req.getTags() != null) po.setTags(req.getTags());
        if (req.getStatus() != null) po.setStatus(req.getStatus());
    }
}
