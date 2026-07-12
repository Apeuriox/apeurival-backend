package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.mapper.VaultItemMapper;
import me.aloic.apeurival.entity.mapper.WorkMapper;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.UserOAuthMapper;
import me.aloic.apeurival.entity.dto.UserDTO;
import me.aloic.apeurival.entity.po.BlogPostPO;
import me.aloic.apeurival.entity.po.OperationLogPO;
import me.aloic.apeurival.entity.po.UploadMetaPO;
import me.aloic.apeurival.entity.po.VaultItemPO;
import me.aloic.apeurival.entity.po.WorkPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.UserOAuthPO;
import me.aloic.apeurival.converter.UserConverter;
import me.aloic.apeurival.enums.RoleEnum;
import me.aloic.apeurival.enums.EntityTypeEnum;
import me.aloic.apeurival.enums.OperationTypeEnum;
import me.aloic.apeurival.scheduled.FileCleanupScheduler;
import me.aloic.apeurival.security.TokenInvalidationStore;
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
    private final TokenInvalidationStore invalidationStore;
    private final UserMapper userMapper;
    private final UserOAuthMapper userOAuthMapper;

    public AdminServiceImpl(OperationLogService operationLogService,
                            ObjectMapper objectMapper,
                            BlogPostMapper blogPostMapper,
                            WorkMapper workMapper,
                            VaultItemMapper vaultItemMapper,
                            FileCleanupScheduler fileCleanupScheduler,
                            TokenInvalidationStore invalidationStore,
                            UserMapper userMapper,
                            UserOAuthMapper userOAuthMapper) {
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
        this.blogPostMapper = blogPostMapper;
        this.workMapper = workMapper;
        this.vaultItemMapper = vaultItemMapper;
        this.fileCleanupScheduler = fileCleanupScheduler;
        this.invalidationStore = invalidationStore;
        this.userMapper = userMapper;
        this.userOAuthMapper = userOAuthMapper;
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

    @Override
    public void invalidateUserTokens(Long userId) {
        requireUser(userId);
        invalidationStore.invalidateAllTokens(userId);
        log.info("Admin invalidated all tokens for user {}", userId);
    }

    @Override
    public Page<UserDTO> listUsers(String query, String role, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be >= 1 and size must be 1..100");
        }
        QueryWrapper<UserPO> wrapper = new QueryWrapper<>();
        if (query != null && !query.isBlank()) {
            wrapper.and(w -> w.like("username", query.trim())
                    .or().like("display_name", query.trim())
                    .or().like("email", query.trim()));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq("role", parseRole(role).name());
        }
        wrapper.orderByDesc("created_at");
        Page<UserPO> users = userMapper.selectPage(new Page<>(page, size), wrapper);
        Page<UserDTO> result = new Page<>(page, size, users.getTotal());
        result.setRecords(users.getRecords().stream().map(this::toUserDTO).toList());
        return result;
    }

    @Override
    public UserDTO getUser(Long userId) {
        return toUserDTO(requireUser(userId));
    }

    @Override
    @Transactional
    public UserDTO updateUserRole(Long userId, String role, Long operatorId) {
        if (userId.equals(operatorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot change their own role");
        }
        UserPO user = requireUser(userId);
        RoleEnum currentRole = RoleEnum.fromString(user.getRole());
        RoleEnum targetRole = parseRole(role);
        if (currentRole == targetRole) return toUserDTO(user);
        if (currentRole == RoleEnum.ADMIN && targetRole != RoleEnum.ADMIN) {
            Long adminCount = userMapper.selectCount(new QueryWrapper<UserPO>().eq("role", RoleEnum.ADMIN.name()));
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one ADMIN must remain");
            }
        }
        user.setRole(targetRole.name());
        userMapper.updateById(user);
        invalidationStore.invalidateAllTokens(userId);
        log.info("Admin {} changed user {} role from {} to {}", operatorId, userId, currentRole, targetRole);
        return toUserDTO(user);
    }

    private UserPO requireUser(Long userId) {
        UserPO user = userMapper.selectById(userId);
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        return user;
    }

    private RoleEnum parseRole(String role) {
        try {
            RoleEnum parsed = RoleEnum.fromString(role);
            if (parsed == null) throw new IllegalArgumentException();
            return parsed;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must be USER, OSU, LIBRARIAN, EDITOR or ADMIN");
        }
    }

    private UserDTO toUserDTO(UserPO user) {
        List<UserOAuthPO> accounts = userOAuthMapper.selectList(
                new QueryWrapper<UserOAuthPO>().eq("user_id", user.getId()));
        return UserConverter.toDTO(user, accounts);
    }
}
