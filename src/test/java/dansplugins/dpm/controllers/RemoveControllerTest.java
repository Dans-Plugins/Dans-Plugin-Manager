package dansplugins.dpm.controllers;

import dansplugins.dpm.controllers.RemoveController.Outcome;
import dansplugins.dpm.controllers.RemoveController.RemovalPreview;
import dansplugins.dpm.controllers.RemoveController.RemovalResult;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.services.DependencyResolutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RemoveControllerTest {

    private static ProjectRecord record(String name) {
        return ProjectRecord.forGitHub(name, "Dans-Plugins", name);
    }

    private static ProjectRecord recordWithHardDependency(String name, String dependency) {
        return ProjectRecord.builder(name, "Dans-Plugins", name)
                .hardDependencies(List.of(dependency))
                .build();
    }

    private static ProjectRecordRepository projectRecordRepository(ProjectRecord... records) {
        ProjectRecordRepository repository = new ProjectRecordRepository();
        for (ProjectRecord record : records) {
            repository.addProjectRecord(record);
        }
        return repository;
    }

    private static VersionRepository versionRepository(Path tempDir) {
        return new VersionRepository(new File(tempDir.toFile(), "dpm-versions.properties"), null);
    }

    private static RemoveController controller(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository,
                                                VersionRepository versionRepository) {
        return new RemoveController(
                projectRecordRepository,
                pluginFileRepository,
                versionRepository,
                new DependencyResolutionService(projectRecordRepository, pluginFileRepository),
                Logger.getLogger("RemoveControllerTest"));
    }

    // -------------------------------------------------------------------------
    // preview()
    // -------------------------------------------------------------------------

    @Test
    void preview_returnsNotInstalledWhenNoJarPresent(@TempDir Path tempDir) {
        ProjectRecord notInstalled = record("NotInstalled");
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(notInstalled);
        RemoveController controller = controller(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        RemovalPreview preview = controller.preview(notInstalled);

        assertFalse(preview.isInstalled());
        assertNull(preview.getJar());
        assertTrue(preview.getDependents().isEmpty());
    }

    @Test
    void preview_returnsJarAndEmptyDependentsWhenInstalledWithNoDependents(@TempDir Path tempDir) throws Exception {
        ProjectRecord installed = record("Installed");
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(installed);
        RemoveController controller = controller(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        RemovalPreview preview = controller.preview(installed);

        assertTrue(preview.isInstalled());
        assertEquals("Installed.jar", preview.getJar().getName());
        assertTrue(preview.getDependents().isEmpty());
    }

    @Test
    void preview_returnsDependentsWhenAnotherInstalledPluginHardDepends(@TempDir Path tempDir) throws Exception {
        ProjectRecord target = record("Target");
        ProjectRecord dependent = recordWithHardDependency("Dependent", "Target");
        new File(tempDir.toFile(), "Target.jar").createNewFile();
        new File(tempDir.toFile(), "Dependent.jar").createNewFile();
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(target, dependent);
        RemoveController controller = controller(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        RemovalPreview preview = controller.preview(target);

        assertEquals(List.of("Dependent"), preview.getDependents());
    }

    // -------------------------------------------------------------------------
    // remove()
    // -------------------------------------------------------------------------

    @Test
    void remove_returnsNotInstalledOutcomeWhenNoJarPresent(@TempDir Path tempDir) {
        ProjectRecord notInstalled = record("NotInstalled");
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(notInstalled);
        RemoveController controller = controller(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        RemovalResult result = controller.remove(notInstalled);

        assertEquals(Outcome.NOT_INSTALLED, result.getOutcome());
        assertNull(result.getJar());
    }

    @Test
    void remove_deletesJarAndRemovesVersionTagOnSuccess(@TempDir Path tempDir) throws Exception {
        ProjectRecord installed = record("Installed");
        File jar = new File(tempDir.toFile(), "Installed.jar");
        jar.createNewFile();
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(installed);
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("Installed", "v1.0.0");
        RemoveController controller = controller(projectRecordRepository, pluginFileRepository, versionRepository);

        RemovalResult result = controller.remove(installed);

        assertEquals(Outcome.DELETED, result.getOutcome());
        assertFalse(jar.exists());
        assertNull(versionRepository.getStoredTag("Installed"));
    }

    @Test
    void remove_includesDependentsInResultWhenAnotherInstalledPluginHardDepends(@TempDir Path tempDir) throws Exception {
        ProjectRecord target = record("Target");
        ProjectRecord dependent = recordWithHardDependency("Dependent", "Target");
        new File(tempDir.toFile(), "Target.jar").createNewFile();
        new File(tempDir.toFile(), "Dependent.jar").createNewFile();
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(target, dependent);
        RemoveController controller = controller(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        RemovalResult result = controller.remove(target);

        assertEquals(Outcome.DELETED, result.getOutcome());
        assertEquals(List.of("Dependent"), result.getDependents());
    }

    @Test
    void remove_returnsDeleteFailedOutcomeWhenFileCannotBeDeleted(@TempDir Path tempDir) throws Exception {
        File readOnlyDir = tempDir.resolve("readonly").toFile();
        readOnlyDir.mkdir();
        File jar = new File(readOnlyDir, "Installed.jar");
        jar.createNewFile();
        // setWritable(false) on the parent dir is a no-op when running as root (common in some CI environments).
        assumeTrue(readOnlyDir.setWritable(false), "Skipped: cannot make directory read-only");

        ProjectRecord installed = record("Installed");
        PluginFileRepository pluginFileRepository = new PluginFileRepository(readOnlyDir.getAbsolutePath() + "/");
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(installed);
        RemoveController controller = controller(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        RemovalResult result = controller.remove(installed);

        assertEquals(Outcome.DELETE_FAILED, result.getOutcome());
        assertTrue(jar.exists());

        readOnlyDir.setWritable(true);
    }

    // -------------------------------------------------------------------------
    // getInstalledPlugins()
    // -------------------------------------------------------------------------

    @Test
    void getInstalledPlugins_returnsOnlyRecordsWithAnInstalledJar(@TempDir Path tempDir) throws Exception {
        ProjectRecord installed = record("Installed");
        ProjectRecord notInstalled = record("NotInstalled");
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(installed, notInstalled);
        RemoveController controller = controller(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        List<ProjectRecord> result = controller.getInstalledPlugins();

        assertEquals(1, result.size());
        assertEquals("Installed", result.get(0).getName());
    }
}
