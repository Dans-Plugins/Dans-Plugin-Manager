package dansplugins.dpm.objects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRecordTest {

    @Test
    void forGitHub_storesName() {
        ProjectRecord r = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        assertEquals("medievalfactions", r.getName());
    }

    @Test
    void forGitHub_storesOwner() {
        ProjectRecord r = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        assertEquals("Dans-Plugins", r.getOwner());
    }

    @Test
    void forGitHub_storesRepo() {
        ProjectRecord r = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        assertEquals("Medieval-Factions", r.getRepo());
    }

    @Test
    void forGitHub_distinctInstancesAreIndependent() {
        ProjectRecord a = ProjectRecord.forGitHub("alpha", "OrgA", "RepoA");
        ProjectRecord b = ProjectRecord.forGitHub("beta", "OrgB", "RepoB");
        assertNotEquals(a.getName(), b.getName());
        assertNotEquals(a.getOwner(), b.getOwner());
        assertNotEquals(a.getRepo(), b.getRepo());
    }

    // -------------------------------------------------------------------------
    // forGitHub() defaults
    // -------------------------------------------------------------------------

    @Test
    void forGitHub_descriptionIsNullByDefault() {
        assertNull(ProjectRecord.forGitHub("x", "o", "r").getDescription());
    }

    @Test
    void forGitHub_hardDependenciesEmptyByDefault() {
        assertTrue(ProjectRecord.forGitHub("x", "o", "r").getHardDependencies().isEmpty());
    }

    @Test
    void forGitHub_softDependenciesEmptyByDefault() {
        assertTrue(ProjectRecord.forGitHub("x", "o", "r").getSoftDependencies().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    @Test
    void builder_setsDescription() {
        ProjectRecord r = ProjectRecord.builder("currencies", "Dans-Plugins", "Currencies")
                .description("Adds configurable in-game currencies.")
                .build();
        assertEquals("Adds configurable in-game currencies.", r.getDescription());
    }

    @Test
    void builder_setsHardDependencies() {
        ProjectRecord r = ProjectRecord.builder("currencies", "Dans-Plugins", "Currencies")
                .hardDependencies(List.of("medievalfactions"))
                .build();
        assertEquals(List.of("medievalfactions"), r.getHardDependencies());
    }

    @Test
    void builder_setsSoftDependencies() {
        ProjectRecord r = ProjectRecord.builder("medievalfactions", "Dans-Plugins", "Medieval-Factions")
                .softDependencies(List.of("mailboxes"))
                .build();
        assertEquals(List.of("mailboxes"), r.getSoftDependencies());
    }

    @Test
    void builder_hardDependenciesAreImmutable() {
        ProjectRecord r = ProjectRecord.builder("currencies", "Dans-Plugins", "Currencies")
                .hardDependencies(List.of("medievalfactions"))
                .build();
        assertThrows(UnsupportedOperationException.class, () -> r.getHardDependencies().add("other"));
    }

    @Test
    void builder_preservesCoreFields() {
        ProjectRecord r = ProjectRecord.builder("currencies", "Dans-Plugins", "Currencies")
                .description("desc")
                .build();
        assertEquals("currencies", r.getName());
        assertEquals("Dans-Plugins", r.getOwner());
        assertEquals("Currencies", r.getRepo());
    }
}
