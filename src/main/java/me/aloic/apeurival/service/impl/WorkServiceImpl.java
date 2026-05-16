package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.WorkDetailDTO;
import me.aloic.apeurival.entity.dto.WorkRequest;
import me.aloic.apeurival.entity.dto.WorkSummaryDTO;
import me.aloic.apeurival.entity.mapper.WorkMapper;
import me.aloic.apeurival.entity.po.WorkPO;
import me.aloic.apeurival.service.WorkService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class WorkServiceImpl implements WorkService {

    private final WorkMapper workMapper;

    public WorkServiceImpl(WorkMapper workMapper) {
        this.workMapper = workMapper;
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
        List<WorkSummaryDTO> dtoList = result.getRecords().stream()
                .map(this::toSummary)
                .toList();

        Page<WorkSummaryDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    public WorkDetailDTO getWork(Long id) {
        WorkPO po = workMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work not found");
        }
        return toDetail(po);
    }

    @Override
    public WorkDetailDTO createWork(WorkRequest req) {
        WorkPO po = new WorkPO();
        applyRequest(po, req);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        workMapper.insert(po);
        return toDetail(po);
    }

    @Override
    public WorkDetailDTO updateWork(Long id, WorkRequest req) {
        WorkPO po = workMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work not found");
        }
        applyRequest(po, req);
        po.setUpdatedAt(LocalDateTime.now());
        workMapper.updateById(po);
        return toDetail(po);
    }

    @Override
    public void deleteWork(Long id) {
        WorkPO po = workMapper.selectById(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work not found");
        }
        workMapper.deleteById(id);
    }

    private WorkSummaryDTO toSummary(WorkPO po) {
        WorkSummaryDTO dto = new WorkSummaryDTO();
        dto.setId(po.getId());
        dto.setTitle(po.getTitle());
        dto.setType(po.getType());
        dto.setCoverUrl(po.getCoverUrl());
        dto.setTags(splitTags(po.getTags()));
        dto.setAuthorName(po.getAuthorName());
        dto.setDate(po.getCreatedAt().toLocalDate());
        return dto;
    }

    private WorkDetailDTO toDetail(WorkPO po) {
        WorkDetailDTO dto = new WorkDetailDTO();
        dto.setId(po.getId());
        dto.setTitle(po.getTitle());
        dto.setDescription(po.getDescription());
        dto.setType(po.getType());
        dto.setContentUrl(po.getContentUrl());
        dto.setCoverUrl(po.getCoverUrl());
        dto.setTags(splitTags(po.getTags()));
        dto.setAuthorName(po.getAuthorName());
        dto.setDate(po.getCreatedAt().toLocalDate());

        if ("VIDEO".equals(po.getType()) && po.getContentUrl() != null) {
            dto.setEmbedUrl("//player.bilibili.com/player.html?bvid=" + po.getContentUrl());
        }
        return dto;
    }

    private void applyRequest(WorkPO po, WorkRequest req) {
        if (req.getTitle() != null) po.setTitle(req.getTitle());
        if (req.getDescription() != null) po.setDescription(req.getDescription());
        if (req.getType() != null) po.setType(req.getType().toUpperCase());
        if (req.getContentUrl() != null) po.setContentUrl(req.getContentUrl());
        if (req.getCoverUrl() != null) po.setCoverUrl(req.getCoverUrl());
        if (req.getTags() != null) po.setTags(req.getTags());
        if (req.getAuthorName() != null) po.setAuthorName(req.getAuthorName());
        if (req.getStatus() != null) po.setStatus(req.getStatus());
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
