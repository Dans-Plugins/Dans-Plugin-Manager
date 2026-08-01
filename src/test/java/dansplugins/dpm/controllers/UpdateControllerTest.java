package dansplugins.dpm.controllers;

import dansplugins.dpm.controllers.UpdateController.Outcome;
import dansplugins.dpm.controllers.UpdateController.PluginResult;
import dansplugins.dpm.controllers.UpdateController.SelectionResult;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.ConfigRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.services.DiscordNotificationService;
import dansplugins.dpm.services.DownloadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UpdateControllerTest {

    private static ProjectRecord record(String name) {
        return ProjectRecord.forGitHub(name, "Dans-Plugins", name);
    }

    private static VersionRepository versionRepository(Path tempDir) {
        return new VersionRepository(new File(tempDir.toFile(), "dpm-versions.properties"), null);
    }

    private static DiscordNotificationService recordingDiscordService(List<String> sent) {
        return new DiscordNotificationService((ConfigRepository) null) {
            @Override
            public void send(String message) {
                sent.add(message);
            }
        };
    }

    private static DownloadService stubDownloadService(Map<String, Integer> resultsByName) {
        return new DownloadService(null, null, null, null) {
            @Override
            public int downloadLatest(ProjectRecord projectRecord, boolean physicallyInstalled) {
                return resultsByName.get(projectRecord.getName());
            }
        };
    }

    private static ProjectRecordRepository projectRecordRepository(ProjectRecord... records) {
        ProjectRecordRepository repository = new ProjectRecordRepository();
        for (ProjectRecord record : records) {
            repository.addProjectRecord(record);
        }
        return repository;
    }

    private static UpdateController controller(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository,
                                                Map<String, Integer> resultsByName, VersionRepository versionRepository,
                                                List<String> sentDiscordMessages) {
        return new UpdateController(
                projectRecordRepository,
                pluginFileRepository,
                stubDownloadService(resultsByName),
                versionRepository,
                recordingDiscordService(sentDiscordMessages),
                Logger.getLogger("UpdateControllerTest"));
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
        UpdateController controller = controller(projectRecordRepository, pluginFileRepository, new HashMap<>(), versionRepository(tempDir), new ArrayList<>());

        List<ProjectRecord> result = controller.getInstalledPlugins();

        assertEquals(1, result.size());
        assertEquals("Installed", result.get(0).getName());
    }

    // -------------------------------------------------------------------------
    // selectForUpdate()
    // -------------------------------------------------------------------------

    @Test
    void selectForUpdate_returnsNotFoundForUnknownPluginName(@TempDir Path tempDir) {
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository();
        UpdateController controller = controller(projectRecordRepository, pluginFileRepository, new HashMap<>(), versionRepository(tempDir), new ArrayList<>());

        SelectionResult result = controller.selectForUpdate(new String[]{"Unknown"});

        assertTrue(result.getToUpdate().isEmpty());
        assertEquals(List.of("Unknown"), result.getNotFound());
        assertTrue(result.getNotInstalled().isEmpty());
    }

    @Test
    void selectForUpdate_returnsNotInstalledForKnownButUninstalledPlugin(@TempDir Path tempDir) {
        ProjectRecord notInstalled = record("NotInstalled");
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(notInstalled);
        UpdateController controller = controller(projectRecordRepository, pluginFileRepository, new HashMap<>(), versionRepository(tempDir), new ArrayList<>());

        SelectionResult result = controller.selectForUpdate(new String[]{"NotInstalled"});

        assertTrue(result.getToUpdate().isEmpty());
        assertTrue(result.getNotFound().isEmpty());
        assertEquals(List.of("NotInstalled"), result.getNotInstalled());
    }

    @Test
    void selectForUpdate_returnsToUpdateForKnownAndInstalledPlugin(@TempDir Path tempDir) throws Exception {
        ProjectRecord installed = record("Installed");
        new File(tempDir.toFile(), "Installed.jar").createNewFile();
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(installed);
        UpdateController controller = controller(projectRecordRepository, pluginFileRepository, new HashMap<>(), versionRepository(tempDir), new ArrayList<>());

        SelectionResult result = controller.selectForUpdate(new String[]{"Installed"});

        assertEquals(List.of(installed), result.getToUpdate());
        assertTrue(result.getNotFound().isEmpty());
        assertTrue(result.getNotInstalled().isEmpty());
    }

    // -------------------------------------------------------------------------
    // runBatch()
    // -------------------------------------------------------------------------

    @Test
    void runBatch_returnsAlreadyUpToDateOutcomeWithStoredTag(@TempDir Path tempDir) throws Exception {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MyPlugin", "v1.2.3");
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.ALREADY_UP_TO_DATE);
        UpdateController controller = controller(projectRecordRepository(), new PluginFileRepository(tempDir.toString()), results, versionRepository, new ArrayList<>());

        List<PluginResult> batchResults = controller.runBatch(List.of(record("MyPlugin")));

        assertEquals(1, batchResults.size());
        assertEquals(Outcome.ALREADY_UP_TO_DATE, batchResults.get(0).getOutcome());
        assertEquals("v1.2.3", batchResults.get(0).getOldTag());
    }

    @Test
    void runBatch_returnsNoReleaseOutcomeWhenNoPublishedRelease(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.NO_RELEASE);
        UpdateController controller = controller(projectRecordRepository(), new PluginFileRepository(tempDir.toString()), results, versionRepository(tempDir), new ArrayList<>());

        List<PluginResult> batchResults = controller.runBatch(List.of(record("MyPlugin")));

        assertEquals(Outcome.NO_RELEASE, batchResults.get(0).getOutcome());
    }

    @Test
    void runBatch_returnsUpdatedOutcomeWithOldAndNewTagOnSuccess(@TempDir Path tempDir) throws Exception {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MyPlugin", "v1.0.0");
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", 4096);
        UpdateController controller = controller(projectRecordRepository(), new PluginFileRepository(tempDir.toString()), results, versionRepository, new ArrayList<>());

        List<PluginResult> batchResults = controller.runBatch(List.of(record("MyPlugin")));

        assertEquals(Outcome.UPDATED, batchResults.get(0).getOutcome());
        assertEquals("v1.0.0", batchResults.get(0).getOldTag());
        assertEquals("v1.0.0", batchResults.get(0).getNewTag(), "stub download doesn't advance the stored tag, so new == old here");
    }

    @Test
    void runBatch_returnsNetworkErrorOutcomeForNetworkFailure(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.NETWORK_ERROR);
        UpdateController controller = controller(projectRecordRepository(), new PluginFileRepository(tempDir.toString()), results, versionRepository(tempDir), new ArrayList<>());

        List<PluginResult> batchResults = controller.runBatch(List.of(record("MyPlugin")));

        assertEquals(Outcome.NETWORK_ERROR, batchResults.get(0).getOutcome());
    }

    @Test
    void runBatch_returnsFileErrorOutcomeForFileWriteFailure(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.FILE_ERROR);
        UpdateController controller = controller(projectRecordRepository(), new PluginFileRepository(tempDir.toString()), results, versionRepository(tempDir), new ArrayList<>());

        List<PluginResult> batchResults = controller.runBatch(List.of(record("MyPlugin")));

        assertEquals(Outcome.FILE_ERROR, batchResults.get(0).getOutcome());
    }

    @Test
    void runBatch_returnsOtherFailureOutcomeForUnrecognizedNegativeResult(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", -99);
        UpdateController controller = controller(projectRecordRepository(), new PluginFileRepository(tempDir.toString()), results, versionRepository(tempDir), new ArrayList<>());

        List<PluginResult> batchResults = controller.runBatch(List.of(record("MyPlugin")));

        assertEquals(Outcome.OTHER_FAILURE, batchResults.get(0).getOutcome());
    }

    @Test
    void runBatch_sendsDiscordSummaryWithCountsAndVersionDiff(@TempDir Path tempDir) throws Exception {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("Updated", "v1.0.0");
        versionRepository.setTag("UpToDate", "v2.0.0");
        Map<String, Integer> results = new HashMap<>();
        results.put("Updated", 2048);
        results.put("UpToDate", DownloadService.ALREADY_UP_TO_DATE);
        results.put("Failed", DownloadService.NETWORK_ERROR);
        List<String> sent = new ArrayList<>();
        UpdateController controller = controller(projectRecordRepository(), new PluginFileRepository(tempDir.toString()), results, versionRepository, sent);

        controller.runBatch(List.of(record("Updated"), record("UpToDate"), record("Failed")));

        assertEquals(1, sent.size());
        String message = sent.get(0);
        assertTrue(message.contains("1 updated"), message);
        assertTrue(message.contains("1 already up to date"), message);
        assertTrue(message.contains("1 failed"), message);
        assertTrue(message.contains("Updated v1.0.0"), message);
    }

    // -------------------------------------------------------------------------
    // versionDiffSuffix()
    // -------------------------------------------------------------------------

    @Test
    void versionDiffSuffix_returnsArrowFormatWhenBothTagsPresent() {
        assertEquals(" v1.0.0 → v2.0.0", UpdateController.versionDiffSuffix("v1.0.0", "v2.0.0"));
    }

    @Test
    void versionDiffSuffix_returnsNewTagOnlyWhenOldTagAbsent() {
        assertEquals(" v2.0.0", UpdateController.versionDiffSuffix(null, "v2.0.0"));
    }

    @Test
    void versionDiffSuffix_returnsEmptyStringWhenBothTagsAbsent() {
        assertEquals("", UpdateController.versionDiffSuffix(null, null));
    }
}
