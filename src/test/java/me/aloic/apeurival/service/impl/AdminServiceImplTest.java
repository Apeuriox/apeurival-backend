package me.aloic.apeurival.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.aloic.apeurival.entity.dto.UserDTO;
import me.aloic.apeurival.entity.mapper.BlogPostMapper;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.UserOAuthMapper;
import me.aloic.apeurival.entity.mapper.VaultItemMapper;
import me.aloic.apeurival.entity.mapper.WorkMapper;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.enums.RoleEnum;
import me.aloic.apeurival.scheduled.FileCleanupScheduler;
import me.aloic.apeurival.security.TokenInvalidationStore;
import me.aloic.apeurival.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {
    @Mock OperationLogService operationLogService;
    @Mock ObjectMapper objectMapper;
    @Mock BlogPostMapper blogPostMapper;
    @Mock WorkMapper workMapper;
    @Mock VaultItemMapper vaultItemMapper;
    @Mock FileCleanupScheduler fileCleanupScheduler;
    @Mock TokenInvalidationStore invalidationStore;
    @Mock UserMapper userMapper;
    @Mock UserOAuthMapper userOAuthMapper;
    @InjectMocks AdminServiceImpl service;

    @Test
    void changesRoleToLibrarianAndInvalidatesExistingTokens() {
        UserPO user = user(2L, RoleEnum.OSU);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(userOAuthMapper.selectList(any())).thenReturn(List.of());

        UserDTO result = service.updateUserRole(2L, "librarian", 1L);

        assertThat(result.getRole()).isEqualTo("LIBRARIAN");
        verify(userMapper).updateById(user);
        verify(invalidationStore).invalidateAllTokens(2L);
    }

    @Test
    void administratorCannotChangeOwnRole() {
        assertThatThrownBy(() -> service.updateUserRole(1L, "EDITOR", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verify(userMapper, never()).updateById(any(UserPO.class));
    }

    @Test
    void lastAdministratorCannotBeDemoted() {
        UserPO user = user(2L, RoleEnum.ADMIN);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.updateUserRole(2L, "EDITOR", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(userMapper, never()).updateById(any(UserPO.class));
        verify(invalidationStore, never()).invalidateAllTokens(2L);
    }

    @Test
    void administratorCanBeDemotedWhenAnotherAdministratorExists() {
        UserPO user = user(2L, RoleEnum.ADMIN);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(2L);
        when(userOAuthMapper.selectList(any())).thenReturn(List.of());

        UserDTO result = service.updateUserRole(2L, "EDITOR", 1L);

        assertThat(result.getRole()).isEqualTo("EDITOR");
        verify(userMapper).updateById(user);
        verify(invalidationStore).invalidateAllTokens(2L);
    }

    @Test
    void rejectsUnknownRole() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, RoleEnum.OSU));
        assertThatThrownBy(() -> service.updateUserRole(2L, "SUPERUSER", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void tokenInvalidationRejectsMissingUser() {
        when(userMapper.selectById(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.invalidateUserTokens(404L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(invalidationStore, never()).invalidateAllTokens(404L);
    }

    private UserPO user(Long id, RoleEnum role) {
        UserPO user = new UserPO();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setRole(role.name());
        return user;
    }
}
