package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.po.OperationLogPO;

public interface OperationLogService {

    void logCreate(String entityType, Long entityId, Long operatorId, Object entity);

    void logUpdate(String entityType, Long entityId, Long operatorId, Object entity, Object previousEntity);

    void logDelete(String entityType, Long entityId, Long operatorId, Object previousEntity);

    Page<OperationLogPO> listLogs(String entityType, Long entityId, int page, int size);

    OperationLogPO getLogById(Long logId);
}
