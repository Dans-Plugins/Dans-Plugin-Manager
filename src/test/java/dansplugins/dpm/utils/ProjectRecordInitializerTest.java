package dansplugins.dpm.utils;

import dansplugins.dpm.factories.ProjectRecordFactory;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRecordInitializerTest {

    private ProjectRecordRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ProjectRecordRepository();
        new ProjectRecordInitializer(new ProjectRecordFactory(repository)).initializeProjectRecords();
    }

    // -------------------------------------------------------------------------
    // initializeProjectRecords() — registry shape
    // -------------------------------------------------------------------------

    @Test
    void initializeProjectRecords_registersEveryManagedPlugin() {
        assertEquals(28, repository.getNumProjectRecords());
    }

    @Test
    void initializeProjectRecords_namesAreUniqueIgnoringCase() {
        Set<String> seen = new HashSet<>();
        for (ProjectRecord record : repository.getAllProjectRecords()) {
            assertTrue(seen.add(record.getName().toLowerCase()), "duplicate name: " + record.getName());
        }
    }

    @Test
    void initializeProjectRecords_namesAreLowercaseAlphanumeric() {
        // The name doubles as the managed JAR filename (<name>.jar) and the /dpm argument.
        for (ProjectRecord record : repository.getAllProjectRecords()) {
            assertTrue(record.getName().matches("[a-z0-9]+"), "unexpected name format: " + record.getName());
        }
    }

    @Test
    void initializeProjectRecords_everyRecordIsOwnedByDansPlugins() {
        for (ProjectRecord record : repository.getAllProjectRecords()) {
            assertEquals("Dans-Plugins", record.getOwner(), record.getName());
        }
    }

    @Test
    void initializeProjectRecords_everyRecordHasRepoAndDescription() {
        for (ProjectRecord record : repository.getAllProjectRecords()) {
            assertNotNull(record.getRepo(), record.getName());
            assertFalse(record.getRepo().isBlank(), record.getName());
            assertNotNull(record.getDescription(), record.getName());
            assertFalse(record.getDescription().isBlank(), record.getName());
        }
    }

    @Test
    void initializeProjectRecords_reposAreUnique() {
        Set<String> seen = new HashSet<>();
        for (ProjectRecord record : repository.getAllProjectRecords()) {
            assertTrue(seen.add(record.getRepo().toLowerCase()), "duplicate repo: " + record.getRepo());
        }
    }

    // -------------------------------------------------------------------------
    // initializeProjectRecords() — dependency graph
    // -------------------------------------------------------------------------

    @Test
    void initializeProjectRecords_everyDependencyNamesARegisteredRecord() {
        // An unregistered hard dependency would surface as an "unknown dependency" warning on /dpm get.
        for (ProjectRecord record : repository.getAllProjectRecords()) {
            List<String> deps = new ArrayList<>(record.getHardDependencies());
            deps.addAll(record.getSoftDependencies());
            for (String dep : deps) {
                assertNotNull(repository.getProjectRecord(dep), record.getName() + " depends on unregistered " + dep);
            }
        }
    }

    @Test
    void initializeProjectRecords_noRecordDependsOnItself() {
        for (ProjectRecord record : repository.getAllProjectRecords()) {
            assertFalse(record.getHardDependencies().contains(record.getName()), record.getName());
            assertFalse(record.getSoftDependencies().contains(record.getName()), record.getName());
        }
    }

    @Test
    void initializeProjectRecords_noDependencyIsBothHardAndSoft() {
        for (ProjectRecord record : repository.getAllProjectRecords()) {
            for (String dep : record.getHardDependencies()) {
                assertFalse(record.getSoftDependencies().contains(dep), record.getName() + " lists " + dep + " twice");
            }
        }
    }

    @Test
    void initializeProjectRecords_declaresMedievalFactionsAsHardDependency() {
        for (String name : List.of("bluemapmedievalfactions", "currencies", "democracy", "fiefs")) {
            assertEquals(List.of("medievalfactions"), repository.getProjectRecord(name).getHardDependencies(), name);
            assertEquals(List.of(), repository.getProjectRecord(name).getSoftDependencies(), name);
        }
    }

    @Test
    void initializeProjectRecords_declaresSoftDependencies() {
        ProjectRecord factions = repository.getProjectRecord("medievalfactions");
        assertEquals(List.of(), factions.getHardDependencies());
        assertEquals(List.of("mailboxes"), factions.getSoftDependencies());

        ProjectRecord engine = repository.getProjectRecord("medievalroleplayengine");
        assertEquals(List.of(), engine.getHardDependencies());
        assertEquals(List.of("medievalfactions", "mailboxes"), engine.getSoftDependencies());
    }

    @Test
    void initializeProjectRecords_recordsWithoutDeclaredDependenciesHaveEmptyLists() {
        ProjectRecord record = repository.getProjectRecord("flycommand");
        assertNotNull(record);
        assertTrue(record.getHardDependencies().isEmpty());
        assertTrue(record.getSoftDependencies().isEmpty());
    }
}
