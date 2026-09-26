package dansplugins.dpm.controllers;

import dansplugins.dpm.controllers.ListController.ListEntry;
import dansplugins.dpm.controllers.ListController.OutdatedEntry;
import dansplugins.dpm.controllers.ListController.Staleness;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.utils.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListControllerTest {

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

    private static ChannelRepository channelRepository(Path tempDir) {
        return new ChannelRepository(new File(tempDir.toFile(), "dpm-channels.properties"), null);
    }

    private static GitHubReleaseRepository stubReleaseRepository(ReleaseInfo release) {
        return new GitHubReleaseRepository(null) {
            @Override
            public ReleaseInfo getReleaseMetadata(String owner, String repo, ReleaseChannel channel) {
                return release;
            }
        };
    }

    private static ListController controller(Path tempDir, VersionRepository versionRepository, ProjectRecord... records) {
        return controller(tempDir, versionRepository, stubReleaseRepository(null), channelRepository(tempDir), records);
    }

    private static ListController controller(Path tempDir, VersionRepository versionRepository,
                                             GitHubReleaseRepository releaseRepository, ChannelRepository channelRepository,
                                             ProjectRecord... records) {
        return new ListController(projectRecordRepository(records), new PluginFileRepository(tempDir.toString()),
                versionRepository, releaseRepository, channelRepository);
    }

    private static ListController outdatedController(Path tempDir, VersionRepository versionRepository, ReleaseInfo release,
                                                     ProjectRecord... records) {
        return controller(tempDir, versionRepository, stubReleaseRepository(release), channelRepository(tempDir), records);
    }

    // -------------------------------------------------------------------------
    // listAll()
    // -------------------------------------------------------------------------

    @Test
    void listAll_returnsEmptyListWhenNoRecords(@TempDir Path tempDir) {
        ListController controller = controller(tempDir, versionRepository(tempDir));

        assertTrue(controller.listAll().isEmpty());
    }

    @Test
    void listAll_marksNotInstalledRecordsWithNoStoredTag(@TempDir Path tempDir) {
        ListController controller = controller(tempDir, versionRepository(tempDir), record("MedievalFactions"));

        List<ListEntry> entries = controller.listAll();

        assertEquals(1, entries.size());
        assertEquals("MedievalFactions", entries.get(0).getRecord().getName());
        assertFalse(entries.get(0).isInstalled());
        assertNull(entries.get(0).getStoredTag());
    }

    @Test
    void listAll_marksInstalledRecordsWithStoredTag(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v1.0.0");
        ListController controller = controller(tempDir, versionRepository, record("MedievalFactions"));

        List<ListEntry> entries = controller.listAll();

        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isInstalled());
        assertEquals("v1.0.0", entries.get(0).getStoredTag());
    }

    @Test
    void listAll_returnsInstalledAndNotInstalledTogether(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        ListController controller = controller(tempDir, versionRepository(tempDir), record("Installed"), record("NotInstalled"));

        List<ListEntry> entries = controller.listAll();

        assertEquals(2, entries.size());
        assertTrue(entries.get(0).isInstalled());
        assertFalse(entries.get(1).isInstalled());
    }

    @Test
    void listAll_leavesStoredTagNullWhenInstalledWithNoRecordedVersion(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        ListController controller = controller(tempDir, versionRepository(tempDir), record("Installed"));

        List<ListEntry> entries = controller.listAll();

        assertTrue(entries.get(0).isInstalled());
        assertNull(entries.get(0).getStoredTag());
    }

    // -------------------------------------------------------------------------
    // listInstalled()
    // -------------------------------------------------------------------------

    @Test
    void listInstalled_returnsEmptyListWhenNothingInstalled(@TempDir Path tempDir) {
        ListController controller = controller(tempDir, versionRepository(tempDir), record("MedievalFactions"));

        assertTrue(controller.listInstalled().isEmpty());
    }

    @Test
    void listInstalled_returnsOnlyInstalledRecordsWithStoredTags(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("Installed", "v2.1.0");
        ListController controller = controller(tempDir, versionRepository, record("Installed"), record("NotInstalled"));

        List<ListEntry> entries = controller.listInstalled();

        assertEquals(1, entries.size());
        assertEquals("Installed", entries.get(0).getRecord().getName());
        assertTrue(entries.get(0).isInstalled());
        assertEquals("v2.1.0", entries.get(0).getStoredTag());
    }

    // -------------------------------------------------------------------------
    // listAvailable()
    // -------------------------------------------------------------------------

    @Test
    void listAvailable_returnsEmptyListWhenEverythingInstalled(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        ListController controller = controller(tempDir, versionRepository(tempDir), record("Installed"));

        assertTrue(controller.listAvailable().isEmpty());
    }

    @Test
    void listAvailable_returnsOnlyRecordsThatAreNotInstalled(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        ListController controller = controller(tempDir, versionRepository(tempDir), record("Installed"), record("NotInstalled"));

        List<ProjectRecord> available = controller.listAvailable();

        assertEquals(1, available.size());
        assertEquals("NotInstalled", available.get(0).getName());
    }

    @Test
    void listAvailable_returnsAllRecordsWhenNothingInstalled(@TempDir Path tempDir) {
        ListController controller = controller(tempDir, versionRepository(tempDir), record("One"), record("Two"));

        assertEquals(2, controller.listAvailable().size());
    }

    // -------------------------------------------------------------------------
    // listOutdated()
    // -------------------------------------------------------------------------

    @Test
    void listOutdated_returnsEmptyListWhenNothingInstalled(@TempDir Path tempDir) {
        ListController controller = outdatedController(tempDir, versionRepository(tempDir),
                new ReleaseInfo("v2.0.0", null), record("MedievalFactions"));

        assertTrue(controller.listOutdated().isEmpty());
    }

    @Test
    void listOutdated_marksPluginOutdatedWhenStoredTagDiffersFromLatest(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v1.0.0");
        ListController controller = outdatedController(tempDir, versionRepository,
                new ReleaseInfo("v2.0.0", null), record("MedievalFactions"));

        List<OutdatedEntry> entries = controller.listOutdated();

        assertEquals(1, entries.size());
        assertEquals(Staleness.OUTDATED, entries.get(0).getStaleness());
        assertEquals("v1.0.0", entries.get(0).getStoredTag());
        assertEquals("v2.0.0", entries.get(0).getLatestTag());
    }

    @Test
    void listOutdated_marksPluginUpToDateWhenStoredTagMatchesLatest(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v2.0.0");
        ListController controller = outdatedController(tempDir, versionRepository,
                new ReleaseInfo("v2.0.0", null), record("MedievalFactions"));

        assertEquals(Staleness.UP_TO_DATE, controller.listOutdated().get(0).getStaleness());
    }

    @Test
    void listOutdated_marksPluginOutdatedWhenNoVersionWasRecorded(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        ListController controller = outdatedController(tempDir, versionRepository(tempDir),
                new ReleaseInfo("v2.0.0", null), record("MedievalFactions"));

        OutdatedEntry entry = controller.listOutdated().get(0);

        assertEquals(Staleness.OUTDATED, entry.getStaleness());
        assertNull(entry.getStoredTag());
    }

    @Test
    void listOutdated_reportsNoReleaseWhenRepositoryPublishesNothing(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        ListController controller = outdatedController(tempDir, versionRepository(tempDir),
                ReleaseInfo.NO_RELEASE, record("MedievalFactions"));

        OutdatedEntry entry = controller.listOutdated().get(0);

        assertEquals(Staleness.NO_RELEASE, entry.getStaleness());
        assertNull(entry.getLatestTag());
    }

    @Test
    void listOutdated_reportsLookupFailedWhenReleaseCannotBeFetched(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v1.0.0");
        ListController controller = outdatedController(tempDir, versionRepository, null, record("MedievalFactions"));

        OutdatedEntry entry = controller.listOutdated().get(0);

        assertEquals(Staleness.LOOKUP_FAILED, entry.getStaleness());
        assertEquals("v1.0.0", entry.getStoredTag());
    }

    @Test
    void listOutdated_checksEachPluginAgainstItsPinnedChannel(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "Stable.jar").createNewFile();
        new File(tempDir.toFile(), "Experimental.jar").createNewFile();
        ChannelRepository channelRepository = channelRepository(tempDir);
        channelRepository.setChannel("Experimental", ReleaseChannel.EXPERIMENTAL);
        List<ReleaseChannel> requested = new ArrayList<>();
        GitHubReleaseRepository releaseRepository = new GitHubReleaseRepository(null) {
            @Override
            public ReleaseInfo getReleaseMetadata(String owner, String repo, ReleaseChannel channel) {
                requested.add(channel);
                return new ReleaseInfo("v1.0.0", null);
            }
        };
        ListController controller = controller(tempDir, versionRepository(tempDir), releaseRepository, channelRepository,
                record("Stable"), record("Experimental"));

        List<OutdatedEntry> entries = controller.listOutdated();

        assertEquals(List.of(ReleaseChannel.STABLE, ReleaseChannel.EXPERIMENTAL), requested);
        assertEquals(ReleaseChannel.STABLE, entries.get(0).getChannel());
        assertEquals(ReleaseChannel.EXPERIMENTAL, entries.get(1).getChannel());
    }

    @Test
    void listOutdated_neverWritesStoredTags(@TempDir Path tempDir) throws Exception {
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v1.0.0");
        ListController controller = outdatedController(tempDir, versionRepository,
                new ReleaseInfo("v2.0.0", null), record("MedievalFactions"));

        controller.listOutdated();

        assertEquals("v1.0.0", versionRepository.getStoredTag("MedievalFactions"));
    }
}
