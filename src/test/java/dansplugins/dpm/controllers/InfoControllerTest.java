package dansplugins.dpm.controllers;

import dansplugins.dpm.controllers.InfoController.PluginInfo;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.repositories.GitHubReleaseRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.utils.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InfoControllerTest {

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

    private static final Logger NO_OP_LOGGER = new Logger(null) {
        @Override public void log(String message) {}
        @Override public void warn(String message) {}
    };

    private static VersionRepository versionRepository(Path tempDir) {
        return new VersionRepository(new File(tempDir.toFile(), "dpm-versions.properties"), NO_OP_LOGGER);
    }

    private static GitHubReleaseRepository stubReleaseRepository(ReleaseInfo release) {
        return new GitHubReleaseRepository(null) {
            @Override
            public ReleaseInfo getLatestReleaseMetadata(String owner, String repo) {
                return release;
            }
        };
    }

    private static InfoController controller(Path tempDir, ReleaseInfo release, VersionRepository versionRepository,
                                             ProjectRecord... records) {
        return new InfoController(projectRecordRepository(records), stubReleaseRepository(release),
                new PluginFileRepository(tempDir.toString()), versionRepository);
    }

    // -------------------------------------------------------------------------
    // getRecord()
    // -------------------------------------------------------------------------

    @Test
    void getRecord_returnsRecordWhenRegistered(@TempDir Path tempDir) {
        InfoController controller = controller(tempDir, null, versionRepository(tempDir), record("MedievalFactions"));

        assertNotNull(controller.getRecord("MedievalFactions"));
    }

    @Test
    void getRecord_returnsRecordCaseInsensitively(@TempDir Path tempDir) {
        InfoController controller = controller(tempDir, null, versionRepository(tempDir), record("MedievalFactions"));

        assertEquals("MedievalFactions", controller.getRecord("medievalfactions").getName());
    }

    @Test
    void getRecord_returnsNullWhenNotRegistered(@TempDir Path tempDir) {
        InfoController controller = controller(tempDir, null, versionRepository(tempDir), record("MedievalFactions"));

        assertNull(controller.getRecord("NotARealPlugin"));
    }

    // -------------------------------------------------------------------------
    // getInfo()
    // -------------------------------------------------------------------------

    @Test
    void getInfo_reportsNotInstalledWhenJarAbsent(@TempDir Path tempDir) {
        ProjectRecord record = record("MedievalFactions");
        InfoController controller = controller(tempDir, new ReleaseInfo("v1.0.0", "url"), versionRepository(tempDir), record);

        PluginInfo info = controller.getInfo(record);

        assertFalse(info.isInstalled());
        assertSame(record, info.getRecord());
    }

    @Test
    void getInfo_reportsInstalledWithStoredTag(@TempDir Path tempDir) throws Exception {
        ProjectRecord record = record("MedievalFactions");
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v1.0.0");
        InfoController controller = controller(tempDir, new ReleaseInfo("v1.0.0", "url"), versionRepository, record);

        PluginInfo info = controller.getInfo(record);

        assertTrue(info.isInstalled());
        assertEquals("v1.0.0", info.getStoredTag());
    }

    @Test
    void getInfo_returnsFetchedRelease(@TempDir Path tempDir) {
        ProjectRecord record = record("MedievalFactions");
        ReleaseInfo release = new ReleaseInfo("v4.6.3", "url", "2026-05-18T12:00:00Z");
        InfoController controller = controller(tempDir, release, versionRepository(tempDir), record);

        assertSame(release, controller.getInfo(record).getRelease());
    }

    @Test
    void getInfo_returnsNullReleaseWhenFetchFails(@TempDir Path tempDir) {
        ProjectRecord record = record("MedievalFactions");
        InfoController controller = controller(tempDir, null, versionRepository(tempDir), record);

        assertNull(controller.getInfo(record).getRelease());
    }

    @Test
    void getInfo_marksRegisteredDependenciesInstalled(@TempDir Path tempDir) throws Exception {
        ProjectRecord dependency = record("Fiefs");
        ProjectRecord record = ProjectRecord.builder("MedievalFactions", "Dans-Plugins", "MedievalFactions")
                .hardDependencies(List.of("Fiefs"))
                .softDependencies(List.of("Currencies"))
                .build();
        new File(tempDir.toFile(), "Fiefs.jar").createNewFile();
        InfoController controller = controller(tempDir, null, versionRepository(tempDir), record, dependency);

        PluginInfo info = controller.getInfo(record);

        assertTrue(info.isDependencyInstalled("Fiefs"));
        assertFalse(info.isDependencyInstalled("Currencies"));
    }

    @Test
    void getInfo_marksUnregisteredDependencyNotInstalled(@TempDir Path tempDir) {
        ProjectRecord record = ProjectRecord.builder("MedievalFactions", "Dans-Plugins", "MedievalFactions")
                .hardDependencies(List.of("SomeThirdPartyPlugin"))
                .build();
        InfoController controller = controller(tempDir, null, versionRepository(tempDir), record);

        assertFalse(controller.getInfo(record).isDependencyInstalled("SomeThirdPartyPlugin"));
    }

    // -------------------------------------------------------------------------
    // PluginInfo.hasPublishedRelease() / isUpToDate()
    // -------------------------------------------------------------------------

    @Test
    void hasPublishedRelease_returnsFalseWhenFetchFailed(@TempDir Path tempDir) {
        ProjectRecord record = record("MedievalFactions");
        InfoController controller = controller(tempDir, null, versionRepository(tempDir), record);

        assertFalse(controller.getInfo(record).hasPublishedRelease());
    }

    @Test
    void hasPublishedRelease_returnsFalseWhenRepoHasNoRelease(@TempDir Path tempDir) {
        ProjectRecord record = record("MedievalFactions");
        InfoController controller = controller(tempDir, ReleaseInfo.NO_RELEASE, versionRepository(tempDir), record);

        assertFalse(controller.getInfo(record).hasPublishedRelease());
    }

    @Test
    void hasPublishedRelease_returnsTrueWhenReleaseFetched(@TempDir Path tempDir) {
        ProjectRecord record = record("MedievalFactions");
        InfoController controller = controller(tempDir, new ReleaseInfo("v1.0.0", "url"), versionRepository(tempDir), record);

        assertTrue(controller.getInfo(record).hasPublishedRelease());
    }

    @Test
    void isUpToDate_returnsTrueWhenStoredTagMatchesLatestRelease(@TempDir Path tempDir) throws Exception {
        ProjectRecord record = record("MedievalFactions");
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v4.6.3");
        InfoController controller = controller(tempDir, new ReleaseInfo("v4.6.3", "url"), versionRepository, record);

        assertTrue(controller.getInfo(record).isUpToDate());
    }

    @Test
    void isUpToDate_returnsFalseWhenStoredTagIsBehindLatestRelease(@TempDir Path tempDir) throws Exception {
        ProjectRecord record = record("MedievalFactions");
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v4.5.0");
        InfoController controller = controller(tempDir, new ReleaseInfo("v4.6.3", "url"), versionRepository, record);

        assertFalse(controller.getInfo(record).isUpToDate());
    }

    @Test
    void isUpToDate_returnsFalseWhenNoTagStored(@TempDir Path tempDir) throws Exception {
        ProjectRecord record = record("MedievalFactions");
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        InfoController controller = controller(tempDir, new ReleaseInfo("v4.6.3", "url"), versionRepository(tempDir), record);

        assertFalse(controller.getInfo(record).isUpToDate());
    }

    @Test
    void isUpToDate_returnsFalseWhenNotInstalledEvenIfTagMatches(@TempDir Path tempDir) throws Exception {
        ProjectRecord record = record("MedievalFactions");
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v4.6.3");
        InfoController controller = controller(tempDir, new ReleaseInfo("v4.6.3", "url"), versionRepository, record);

        assertFalse(controller.getInfo(record).isUpToDate());
    }
}
