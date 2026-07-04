package me.aloic.apeurival.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.po.OperationLogPO;
import me.aloic.apeurival.entity.po.UploadMetaPO;

import java.util.List;
import java.util.Map;

public interface AdminService {

    Page<OperationLogPO> listOperationLogs(String entityType, Long entityId, int page, int size);

    Map<String, Object> revertOperation(Long logId, Long operatorId);

    List<UploadMetaPO> listUnboundUploads();

    int bindAndCleanUploads();

    void invalidateUserTokens(Long userId);
}
