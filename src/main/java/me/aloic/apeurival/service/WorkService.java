package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.dto.WorkDetailDTO;
import me.aloic.apeurival.entity.dto.WorkRequest;
import me.aloic.apeurival.entity.dto.WorkSummaryDTO;

public interface WorkService {

    Page<WorkSummaryDTO> listPublishedWorks(String type, int page, int size);

    WorkDetailDTO getWork(Long id);

    WorkDetailDTO createWork(WorkRequest request);

    WorkDetailDTO updateWork(Long id, WorkRequest request);

    void deleteWork(Long id);
}
