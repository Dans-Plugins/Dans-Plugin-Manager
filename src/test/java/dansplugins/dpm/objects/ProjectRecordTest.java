package dansplugins.dpm.objects;

import org.junit.jupiter.api.Test;

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
}
