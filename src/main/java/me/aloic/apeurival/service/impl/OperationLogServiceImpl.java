package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.mapper.OperationLogMapper;
import me.aloic.apeurival.entity.po.OperationLogPO;
import me.aloic.apeurival.enums.OperationTypeEnum;
import me.aloic.apeurival.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper,
                                   ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void logCreate(String entityType, Long entityId, Long operatorId, Object entity) {
        OperationLogPO po = buildLog(entityType, entityId, OperationTypeEnum.CREATE, operatorId);
        po.setEntitySnapshot(serializeObject(entity));
        operationLogMapper.insert(po);
        log.info("Operation log: {} CREATE id={} by operator {}", entityType, entityId, operatorId);
    }

    @Override
    public void logUpdate(String entityType, Long entityId, Long operatorId, Object entity, Object previousEntity) {
        OperationLogPO po = buildLog(entityType, entityId, OperationTypeEnum.UPDATE, operatorId);
        po.setEntitySnapshot(serializeObject(entity));
        po.setPreviousSnapshot(serializeObject(previousEntity));
        operationLogMapper.insert(po);
        log.info("Operation log: {} UPDATE id={} by operator {}", entityType, entityId, operatorId);
    }

    @Override
    public void logDelete(String entityType, Long entityId, Long operatorId, Object previousEntity) {
        OperationLogPO po = buildLog(entityType, entityId, OperationTypeEnum.DELETE, operatorId);
        po.setPreviousSnapshot(serializeObject(previousEntity));
        operationLogMapper.insert(po);
        log.info("Operation log: {} DELETE id={} by operator {}", entityType, entityId, operatorId);
    }

    @Override
    public Page<OperationLogPO> listLogs(String entityType, Long entityId, int page, int size) {
        Page<OperationLogPO> poPage = new Page<>(page, size);
        QueryWrapper<OperationLogPO> wrapper = new QueryWrapper<>();
        if (entityType != null) {
            wrapper.eq("entity_type", entityType.toUpperCase());
        }
        if (entityId != null) {
            wrapper.eq("entity_id", entityId);
        }
        wrapper.orderByDesc("created_at");
        return operationLogMapper.selectPage(poPage, wrapper);
    }

    @Override
    public OperationLogPO getLogById(Long logId) {
        return operationLogMapper.selectById(logId);
    }

    private OperationLogPO buildLog(String entityType, Long entityId, OperationTypeEnum op, Long operatorId) {
        OperationLogPO po = new OperationLogPO();
        po.setEntityType(entityType.toUpperCase());
        po.setEntityId(entityId);
        po.setOperation(op.name());
        po.setOperatorId(operatorId);
        po.setCreatedAt(LocalDateTime.now());
        return po;
    }

    private String serializeObject(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize entity snapshot", e);
        }
    }
}

