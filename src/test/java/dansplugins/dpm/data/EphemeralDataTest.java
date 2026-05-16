package dansplugins.dpm.data;

import dansplugins.dpm.objects.ProjectRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EphemeralDataTest {

    private EphemeralData data;

    @BeforeEach
    void setUp() {
        data = new EphemeralData();
        data.addProjectRecord(ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"));
        data.addProjectRecord(ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies"));
    }

    // -------------------------------------------------------------------------
    // getProjectRecord()
    // -------------------------------------------------------------------------

    @Test
    void getProjectRecord_exactMatch() {
        assertNotNull(data.getProjectRecord("medievalfactions"));
    }

    @Test
    void getProjectRecord_caseInsensitive() {
        assertNotNull(data.getProjectRecord("MedievalFactions"));
        assertNotNull(data.getProjectRecord("CURRENCIES"));
    }

    @Test
    void getProjectRecord_returnsNullForUnknown() {
        assertNull(data.getProjectRecord("notaplugin"));
    }

    // -------------------------------------------------------------------------
    // getAllProjectRecords()
    // -------------------------------------------------------------------------

    @Test
    void getAllProjectRecords_returnsAllRecords() {
        assertEquals(2, data.getAllProjectRecords().size());
    }

    @Test
    void getAllProjectRecords_returnsDefensiveCopy() {
        List<ProjectRecord> copy = data.getAllProjectRecords();
        copy.clear();
        assertEquals(2, data.getAllProjectRecords().size());
    }

    // -------------------------------------------------------------------------
    // getNumProjectRecords()
    // -------------------------------------------------------------------------

    @Test
    void getNumProjectRecords_reflectsCurrentCount() {
        assertEquals(2, data.getNumProjectRecords());
        data.addProjectRecord(ProjectRecord.forGitHub("flycommand", "Dans-Plugins", "Fly-Command"));
        assertEquals(3, data.getNumProjectRecords());
    }

    // -------------------------------------------------------------------------
    // removeProjectRecord()
    // -------------------------------------------------------------------------

    @Test
    void removeProjectRecord_decreasesCount() {
        ProjectRecord r = data.getProjectRecord("currencies");
        assertTrue(data.removeProjectRecord(r));
        assertEquals(1, data.getNumProjectRecords());
    }

    @Test
    void removeProjectRecord_returnsFalseForAbsentRecord() {
        ProjectRecord stranger = ProjectRecord.forGitHub("nothere", "Org", "Repo");
        assertFalse(data.removeProjectRecord(stranger));
    }
}
