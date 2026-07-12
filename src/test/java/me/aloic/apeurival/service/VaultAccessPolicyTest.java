package me.aloic.apeurival.service;

import me.aloic.apeurival.entity.mapper.VaultGroupMemberMapper;
import me.aloic.apeurival.entity.po.VaultItemPO;
import me.aloic.apeurival.enums.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VaultAccessPolicyTest {
    private VaultGroupMemberMapper memberMapper;
    private VaultAccessPolicy policy;

    @BeforeEach
    void setUp() {
        memberMapper = mock(VaultGroupMemberMapper.class);
        policy = new VaultAccessPolicy(memberMapper);
    }

    @Test
    void userWithoutOauthCannotReadPublicItem() {
        assertDenied(item("PUBLIC", null, 9L), 1L, RoleEnum.USER);
    }

    @ParameterizedTest
    @CsvSource({"USER", "OSU", "LIBRARIAN"})
    void rolesBelowEditorCannotWriteVaultItems(RoleEnum role) {
        assertThatThrownBy(() -> policy.requireVaultWrite(role))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @ParameterizedTest
    @CsvSource({"EDITOR", "ADMIN"})
    void editorAndAdminCanWriteVaultItems(RoleEnum role) {
        assertThatCode(() -> policy.requireVaultWrite(role)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} reading {1} should be allowed={2}")
    @CsvSource({
            "OSU,       PUBLIC,      true",
            "OSU,       RESTRICTED,  false",
            "OSU,       EDITOR_ONLY, false",
            "OSU,       PRIVATE,     false",
            "LIBRARIAN, PUBLIC,      true",
            "LIBRARIAN, RESTRICTED,  true",
            "LIBRARIAN, EDITOR_ONLY, false",
            "LIBRARIAN, PRIVATE,     false",
            "EDITOR,    PUBLIC,      true",
            "EDITOR,    RESTRICTED,  true",
            "EDITOR,    EDITOR_ONLY, true",
            "EDITOR,    PRIVATE,     false",
            "ADMIN,     PUBLIC,      true",
            "ADMIN,     RESTRICTED,  true",
            "ADMIN,     EDITOR_ONLY, true",
            "ADMIN,     PRIVATE,     true"
    })
    void enforcesNonOwnerVisibilityMatrix(RoleEnum role, String visibility, boolean allowed) {
        VaultItemPO item = item(visibility, null, 9L);
        if (allowed) {
            assertThatCode(() -> policy.requireItemRead(item, 1L, role)).doesNotThrowAnyException();
        } else {
            assertDenied(item, 1L, role);
        }
    }

    @Test
    void librarianCanReadRestrictedItemOutsideGroup() {
        assertThatCode(() -> policy.requireItemRead(
                item("RESTRICTED", null, 9L), 1L, RoleEnum.LIBRARIAN)).doesNotThrowAnyException();
    }

    @Test
    void librarianCanReadRestrictedItemInGroupWithoutMembership() {
        when(memberMapper.exists(any())).thenReturn(false);
        assertThatCode(() -> policy.requireItemRead(
                item("RESTRICTED", 7L, 9L), 1L, RoleEnum.LIBRARIAN)).doesNotThrowAnyException();
    }

    @Test
    void osuCannotBrowseGroupWithoutMembership() {
        when(memberMapper.exists(any())).thenReturn(false);
        assertThatThrownBy(() -> policy.requireGroupBrowse(7L, 1L, RoleEnum.OSU))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void osuCanBrowseGroupWithMembership() {
        when(memberMapper.exists(any())).thenReturn(true);
        assertThatCode(() -> policy.requireGroupBrowse(7L, 1L, RoleEnum.OSU))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"LIBRARIAN", "EDITOR"})
    void elevatedReadersStillCannotWriteToGroupWithoutMembership(RoleEnum role) {
        when(memberMapper.exists(any())).thenReturn(false);
        assertThatThrownBy(() -> policy.requireGroupWrite(7L, 1L, role))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void adminCanWriteToGroupWithoutMembership() {
        when(memberMapper.exists(any())).thenReturn(false);
        assertThatCode(() -> policy.requireGroupWrite(7L, 1L, RoleEnum.ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    void groupMemberStillCannotReadPrivateItemOwnedBySomeoneElse() {
        when(memberMapper.exists(any())).thenReturn(true);
        assertDenied(item("PRIVATE", 7L, 9L), 1L, RoleEnum.OSU);
    }

    @Test
    void ownerCanReadOwnPrivateItem() {
        assertThatCode(() -> policy.requireItemRead(
                item("PRIVATE", null, 1L), 1L, RoleEnum.OSU)).doesNotThrowAnyException();
    }

    @Test
    void adminCanReadPrivateItem() {
        assertThatCode(() -> policy.requireItemRead(
                item("PRIVATE", 7L, 9L), 1L, RoleEnum.ADMIN)).doesNotThrowAnyException();
    }

    @Test
    void legacyMembersCannotBeWritten() {
        assertThatThrownBy(() -> policy.normalizeWritableVisibility("MEMBERS"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "public, PUBLIC",
            "RESTRICTED, RESTRICTED",
            "editor_only, EDITOR_ONLY",
            "PRIVATE, PRIVATE"
    })
    void normalizesSupportedWritableVisibilities(String input, String expected) {
        org.assertj.core.api.Assertions.assertThat(policy.normalizeWritableVisibility(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"UNKNOWN", "' '", "ADMIN_ONLY"})
    void rejectsUnknownWritableVisibilities(String visibility) {
        assertThatThrownBy(() -> policy.normalizeWritableVisibility(visibility))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void nullVisibilityDefaultsToPublicOnCreate() {
        org.assertj.core.api.Assertions.assertThat(policy.normalizeWritableVisibility(null))
                .isEqualTo("PUBLIC");
    }

    private void assertDenied(VaultItemPO item, Long userId, RoleEnum role) {
        assertThatThrownBy(() -> policy.requireItemRead(item, userId, role))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    private VaultItemPO item(String visibility, Long groupId, Long ownerId) {
        VaultItemPO item = new VaultItemPO();
        item.setVisibility(visibility);
        item.setGroupId(groupId);
        item.setOwnerId(ownerId);
        return item;
    }
}
