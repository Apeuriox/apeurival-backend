package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.WorkDetailDTO;
import me.aloic.apeurival.entity.dto.WorkRequest;
import me.aloic.apeurival.entity.dto.WorkSummaryDTO;

public interface WorkService {

    Page<WorkSummaryDTO> listPublishedWorks(String type, Long authorId, String sort, int page, int size);

    WorkDetailDTO getWork(Long id);

    WorkDetailDTO createWork(WorkRequest request, Long authorId);

    WorkDetailDTO updateWork(Long id, WorkRequest request, Long authorId);

    void deleteWork(Long id, Long userId);
}
