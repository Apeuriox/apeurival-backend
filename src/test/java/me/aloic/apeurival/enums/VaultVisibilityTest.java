package me.aloic.apeurival.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VaultVisibilityTest {
    @Test
    void exposesExpectedVisibilityForEachRole() {
        assertThat(VaultVisibility.visibleDatabaseValues(RoleEnum.OSU))
                .containsExactly("PUBLIC", "MEMBERS");
        assertThat(VaultVisibility.visibleDatabaseValues(RoleEnum.LIBRARIAN))
                .containsExactly("PUBLIC", "RESTRICTED", "MEMBERS");
        assertThat(VaultVisibility.visibleDatabaseValues(RoleEnum.EDITOR))
                .containsExactly("PUBLIC", "RESTRICTED", "EDITOR_ONLY", "MEMBERS");
        assertThat(VaultVisibility.visibleDatabaseValues(RoleEnum.ADMIN))
                .containsExactly("PUBLIC", "RESTRICTED", "EDITOR_ONLY", "PRIVATE", "MEMBERS");
    }

    @Test
    void privateIsNotGenerallyVisibleBelowAdmin() {
        assertThat(VaultVisibility.PRIVATE.isVisibleTo(RoleEnum.EDITOR)).isFalse();
        assertThat(VaultVisibility.PRIVATE.isVisibleTo(RoleEnum.ADMIN)).isTrue();
    }
}
