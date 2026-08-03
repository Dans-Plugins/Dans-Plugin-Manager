package dansplugins.dpm.controllers;

import dansplugins.dpm.controllers.StatsController.Stats;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StatsControllerTest {

    private static ProjectRecord record(String name) {
        return ProjectRecord.forGitHub(name, "Dans-Plugins", name);
    }

    private static ProjectRecordRepository projectRecordRepository(ProjectRecord... records) {
        ProjectRecordRepository repository = new ProjectRecordRepository();
        for (ProjectRecord record : records) {
            repository.addProjectRecord(record);
        }
        return repository;
    }

    // -------------------------------------------------------------------------
    // getStats()
    // -------------------------------------------------------------------------

    @Test
    void getStats_returnsAllZeroesWhenNoRecords(@TempDir Path tempDir) {
        ProjectRecordRepository projectRecordRepository = projectRecordRepository();
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        StatsController controller = new StatsController(projectRecordRepository, pluginFileRepository);

        Stats stats = controller.getStats();

        assertEquals(0, stats.getTotal());
        assertEquals(0, stats.getInstalled());
        assertEquals(0, stats.getAvailable());
    }

    @Test
    void getStats_countsInstalledAndAvailableSeparately(@TempDir Path tempDir) throws Exception {
        ProjectRecord installed = record("Installed");
        ProjectRecord notInstalled = record("NotInstalled");
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(installed, notInstalled);
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        StatsController controller = new StatsController(projectRecordRepository, pluginFileRepository);

        Stats stats = controller.getStats();

        assertEquals(2, stats.getTotal());
        assertEquals(1, stats.getInstalled());
        assertEquals(1, stats.getAvailable());
    }
}
