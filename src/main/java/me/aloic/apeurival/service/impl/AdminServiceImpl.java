package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.mapper.VaultItemMapper;
import me.aloic.apeurival.entity.mapper.WorkMapper;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.OperationLogPO;
import me.aloic.apeurival.entity.po.UploadMetaPO;
import me.aloic.apeurival.entity.po.VaultItemPO;
import me.aloic.apeurival.entity.po.WorkPO;
import me.aloic.apeurival.enums.EntityTypeEnum;
import me.aloic.apeurival.enums.OperationTypeEnum;
import me.aloic.apeurival.scheduled.FileCleanupScheduler;
import me.aloic.apeurival.service.AdminService;
import me.aloic.apeurival.service.OperationLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;
    private final BlogPostMapper blogPostMapper;
    private final WorkMapper workMapper;
    private final VaultItemMapper vaultItemMapper;
    private final FileCleanupScheduler fileCleanupScheduler;

    public AdminServiceImpl(OperationLogService operationLogService,
                            ObjectMapper objectMapper,
                            BlogPostMapper blogPostMapper,
                            WorkMapper workMapper,
                            VaultItemMapper vaultItemMapper,
                            FileCleanupScheduler fileCleanupScheduler) {
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
        this.blogPostMapper = blogPostMapper;
        this.workMapper = workMapper;
        this.vaultItemMapper = vaultItemMapper;
        this.fileCleanupScheduler = fileCleanupScheduler;
    }

    @Override
    public Page<OperationLogPO> listOperationLogs(String entityType, Long entityId, int page, int size) {
        return operationLogService.listLogs(entityType, entityId, page, size);
    }

    @Override
    @Transactional
    public Map<String, Object> revertOperation(Long logId, Long operatorId) {
        OperationLogPO logEntry = operationLogService.getLogById(logId);
        if (logEntry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation log not found");
        }

        EntityTypeEnum et = EntityTypeEnum.fromCode(logEntry.getEntityType());
        OperationTypeEnum op = OperationTypeEnum.valueOf(logEntry.getOperation());
        Long entityId = logEntry.getEntityId();

        return switch (op) {
            case CREATE -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot revert CREATE. Delete the entity directly instead.");
            case UPDATE -> {
                if (logEntry.getPreviousSnapshot() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No previous snapshot available");
                }
                Object restored = restoreEntity(et, entityId, logEntry.getPreviousSnapshot());
                operationLogService.logCreate(et.getCode(), entityId, operatorId, restored);
                log.info("Reverted {} UPDATE id={} by operator {}", et.getCode(), entityId, operatorId);
                yield Map.of("reverted", true, "entityId", entityId, "entityType", et.getCode(),
                        "from", "UPDATE", "to", "restored");
            }

            case DELETE -> {
                if (logEntry.getPreviousSnapshot() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No snapshot available");
                }
                Object restored = restoreEntity(et, entityId, logEntry.getPreviousSnapshot());
                operationLogService.logCreate(et.getCode(), entityId, operatorId, restored);
                log.info("Reverted {} DELETE id={} by operator {}", et.getCode(), entityId, operatorId);
                yield Map.of("reverted", true, "entityId", entityId, "entityType", et.getCode(),
                        "from", "DELETE", "to", "restored");
            }
        };
    }

    private Object restoreEntity(EntityTypeEnum et, Long entityId, String snapshot) {
        return switch (et) {
            case POST -> {
                BlogPostPO po = deserializeJson(snapshot, BlogPostPO.class);
                if (blogPostMapper.selectById(entityId) != null) {
                    blogPostMapper.updateById(po);
                } else {
                    blogPostMapper.insert(po);
                }
                yield po;
            }
            case WORK -> {
                WorkPO po = deserializeJson(snapshot, WorkPO.class);
                if (workMapper.selectById(entityId) != null) {
                    workMapper.updateById(po);
                } else {
                    workMapper.insert(po);
                }
                yield po;
            }
            case VAULT_ITEM -> {
                VaultItemPO po = deserializeJson(snapshot, VaultItemPO.class);
                if (vaultItemMapper.selectById(entityId) != null) {
                    vaultItemMapper.updateById(po);
                } else {
                    vaultItemMapper.insert(po);
                }
                yield po;
            }
        };
    }

    private <T> T deserializeJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize snapshot", e);
        }
    }

    @Override
    public List<UploadMetaPO> listUnboundUploads() {
        return fileCleanupScheduler.listUnboundFiles();
    }

    @Override
    public int bindAndCleanUploads() {
        int bound = fileCleanupScheduler.bindReferencedFiles();
        int removed = fileCleanupScheduler.cleanExpiredUnboundFiles();
        return removed;
    }
}
