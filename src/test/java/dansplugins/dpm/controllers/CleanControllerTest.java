package dansplugins.dpm.controllers;

import dansplugins.dpm.controllers.CleanController.CleanResult;
import dansplugins.dpm.controllers.CleanController.Conflict;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CleanControllerTest {

    private static ProjectRecordRepository projectRecordRepository(ProjectRecord... records) {
        ProjectRecordRepository repository = new ProjectRecordRepository();
        for (ProjectRecord record : records) {
            repository.addProjectRecord(record);
        }
        return repository;
    }

    private static CleanController controller(Path tempDir, ProjectRecord... records) {
        return new CleanController(
                projectRecordRepository(records),
                new PluginFileRepository(tempDir.toString()),
                Logger.getLogger("CleanControllerTest"));
    }

    private static void createJar(Path dir, String name) throws IOException {
        Files.createFile(dir.resolve(name));
    }

    // A directory named like a JAR is still returned by the conflict scan (the scan filters on the
    // filename only), and File.delete() refuses to remove a non-empty directory for any user —
    // including root. That makes it a delete failure that reproduces everywhere, unlike a
    // read-only parent directory, whose permission bits root bypasses (see issue #120).
    private static void createUndeletableJar(Path dir, String name) throws IOException {
        Path fakeJar = Files.createDirectory(dir.resolve(name));
        Files.createFile(fakeJar.resolve("occupant.txt"));
    }

    // -------------------------------------------------------------------------
    // findConflicts()
    // -------------------------------------------------------------------------

    @Test
    void findConflicts_returnsEmptyListWhenNoDuplicatesPresent(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "medievalfactions.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"));

        assertTrue(controller.findConflicts().isEmpty());
    }

    @Test
    void findConflicts_pairsEachDuplicateJarWithThePluginItDuplicates(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "medievalfactions.jar");
        createJar(tempDir, "Medieval-Factions-4.6.3.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"));

        List<Conflict> conflicts = controller.findConflicts();

        assertEquals(1, conflicts.size());
        assertEquals("medievalfactions", conflicts.get(0).getPluginName());
        assertEquals("Medieval-Factions-4.6.3.jar", conflicts.get(0).getJar().getName());
    }

    @Test
    void findConflicts_flattensDuplicatesAcrossMultiplePlugins(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "medievalfactions.jar");
        createJar(tempDir, "Medieval-Factions-4.6.3.jar");
        createJar(tempDir, "currencies.jar");
        createJar(tempDir, "Currencies-2.1.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"),
                ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies"));

        List<Conflict> conflicts = controller.findConflicts();

        assertEquals(2, conflicts.size());
    }

    @Test
    void findConflicts_doesNotReportTheManagedJarItself(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "currencies.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies"));

        assertTrue(controller.findConflicts().isEmpty());
    }

    // -------------------------------------------------------------------------
    // clean()
    // -------------------------------------------------------------------------

    @Test
    void clean_returnsEmptyResultWhenNoDuplicatesPresent(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "medievalfactions.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"));

        CleanResult result = controller.clean();

        assertTrue(result.isEmpty());
        assertTrue(result.getRemoved().isEmpty());
        assertTrue(result.getFailed().isEmpty());
    }

    @Test
    void clean_deletesDuplicateJarAndReportsItAsRemoved(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "medievalfactions.jar");
        createJar(tempDir, "Medieval-Factions-4.6.3.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"));

        CleanResult result = controller.clean();

        assertEquals(1, result.getRemoved().size());
        assertTrue(result.getFailed().isEmpty());
        assertFalse(result.isEmpty());
        assertEquals("Medieval-Factions-4.6.3.jar", result.getRemoved().get(0).getJar().getName());
        assertFalse(new File(tempDir.toFile(), "Medieval-Factions-4.6.3.jar").exists());
    }

    @Test
    void clean_leavesTheManagedJarInPlace(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "medievalfactions.jar");
        createJar(tempDir, "Medieval-Factions-4.6.3.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"));

        controller.clean();

        assertTrue(new File(tempDir.toFile(), "medievalfactions.jar").exists());
    }

    @Test
    void clean_reportsUndeletableDuplicateAsFailedRatherThanRemoved(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "medievalfactions.jar");
        createUndeletableJar(tempDir, "Medieval-Factions-4.6.3.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"));

        CleanResult result = controller.clean();

        assertTrue(result.getRemoved().isEmpty());
        assertEquals(1, result.getFailed().size());
        assertFalse(result.isEmpty());
        assertEquals("Medieval-Factions-4.6.3.jar", result.getFailed().get(0).getJar().getName());
        assertTrue(new File(tempDir.toFile(), "Medieval-Factions-4.6.3.jar").exists());
    }

    @Test
    void clean_separatesRemovedFromFailedWhenBothOccur(@TempDir Path tempDir) throws IOException {
        createJar(tempDir, "medievalfactions.jar");
        createJar(tempDir, "Medieval-Factions-4.6.3.jar");
        createJar(tempDir, "currencies.jar");
        createUndeletableJar(tempDir, "Currencies-2.1.jar");
        CleanController controller = controller(tempDir,
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"),
                ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies"));

        CleanResult result = controller.clean();

        assertEquals(1, result.getRemoved().size());
        assertEquals("medievalfactions", result.getRemoved().get(0).getPluginName());
        assertEquals(1, result.getFailed().size());
        assertEquals("currencies", result.getFailed().get(0).getPluginName());
    }
}
