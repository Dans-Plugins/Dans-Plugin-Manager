package dansplugins.dpm.controllers;

import dansplugins.dpm.controllers.GetController.DependencyResolutionResult;
import dansplugins.dpm.controllers.GetController.Outcome;
import dansplugins.dpm.controllers.GetController.PluginResult;
import dansplugins.dpm.controllers.GetController.Target;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.repositories.ConfigRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.services.DependencyResolutionService;
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
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class GetControllerTest {

    private static ProjectRecord record(String name) {
        return ProjectRecord.forGitHub(name, "Dans-Plugins", name);
    }

    private static VersionRepository versionRepository(Path tempDir) {
        return new VersionRepository(new File(tempDir.toFile(), "dpm-versions.properties"), null);
    }

    private static ChannelRepository channelRepository(Path tempDir) {
        return new ChannelRepository(new File(tempDir.toFile(), "dpm-channels.properties"), null);
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
        return stubDownloadService(resultsByName, new ArrayList<>());
    }

    // Records the channel each download was attempted on, so channel routing can be asserted.
    private static DownloadService stubDownloadService(Map<String, Integer> resultsByName, List<ReleaseChannel> channelsUsed) {
        return new DownloadService(null, null, null, null) {
            @Override
            public int downloadLatest(ProjectRecord projectRecord, ReleaseChannel channel) {
                channelsUsed.add(channel);
                return resultsByName.get(projectRecord.getName());
            }
        };
    }

    private static GetController controller(Map<String, Integer> resultsByName, VersionRepository versionRepository,
                                            ChannelRepository channelRepository, List<String> sentDiscordMessages) {
        return new GetController(
                stubDownloadService(resultsByName),
                new DependencyResolutionService(null, null),
                versionRepository,
                channelRepository,
                recordingDiscordService(sentDiscordMessages),
                Logger.getLogger("GetControllerTest"));
    }

    // -------------------------------------------------------------------------
    // resolveDependencies()
    // -------------------------------------------------------------------------

    @Test
    void resolveDependencies_seedsResolvedSetWithLowercaseRecordNames(@TempDir Path tempDir) {
        List<Set<String>> capturedResolvedSets = new ArrayList<>();
        DependencyResolutionService fakeResolution = new DependencyResolutionService(null, null) {
            @Override
            public void resolve(List<ProjectRecord> toProcess, Set<String> resolved,
                                List<ProjectRecord> depsToFetch, List<String> unknownDeps) {
                capturedResolvedSets.add(new java.util.HashSet<>(resolved));
            }
        };
        GetController controller = new GetController(
                stubDownloadService(new HashMap<>()), fakeResolution, versionRepository(tempDir),
                channelRepository(tempDir), recordingDiscordService(new ArrayList<>()), Logger.getLogger("GetControllerTest"));

        controller.resolveDependencies(List.of(record("MedievalFactions")));

        assertEquals(1, capturedResolvedSets.size());
        assertTrue(capturedResolvedSets.get(0).contains("medievalfactions"));
    }

    @Test
    void resolveDependencies_returnsDepsToFetchAndUnknownDepsFromService(@TempDir Path tempDir) {
        ProjectRecord dep = record("Currencies");
        DependencyResolutionService fakeResolution = new DependencyResolutionService(null, null) {
            @Override
            public void resolve(List<ProjectRecord> toProcess, Set<String> resolved,
                                List<ProjectRecord> depsToFetch, List<String> unknownDeps) {
                depsToFetch.add(dep);
                unknownDeps.add("SomeUnmanagedPlugin");
            }
        };
        GetController controller = new GetController(
                stubDownloadService(new HashMap<>()), fakeResolution, versionRepository(tempDir),
                channelRepository(tempDir), recordingDiscordService(new ArrayList<>()), Logger.getLogger("GetControllerTest"));

        DependencyResolutionResult result = controller.resolveDependencies(List.of(record("MedievalFactions")));

        assertEquals(List.of(dep), result.getDepsToFetch());
        assertEquals(List.of("SomeUnmanagedPlugin"), result.getUnknownDeps());
    }

    // -------------------------------------------------------------------------
    // download()
    // -------------------------------------------------------------------------

    @Test
    void download_returnsNoReleaseOutcomeWhenNoPublishedRelease(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.NO_RELEASE);
        GetController controller = controller(results, versionRepository(tempDir), channelRepository(tempDir), new ArrayList<>());

        PluginResult result = controller.download(record("MyPlugin"));

        assertEquals(Outcome.NO_RELEASE, result.getOutcome());
        assertNull(result.getStoredTag());
    }

    @Test
    void download_returnsAlreadyUpToDateOutcomeWithStoredTag(@TempDir Path tempDir) throws Exception {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MyPlugin", "v1.2.3");
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.ALREADY_UP_TO_DATE);
        GetController controller = controller(results, versionRepository, channelRepository(tempDir), new ArrayList<>());

        PluginResult result = controller.download(record("MyPlugin"));

        assertEquals(Outcome.ALREADY_UP_TO_DATE, result.getOutcome());
        assertEquals("v1.2.3", result.getStoredTag());
    }

    @Test
    void download_returnsNetworkErrorOutcomeAndSendsDiscordNotification(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.NETWORK_ERROR);
        List<String> sent = new ArrayList<>();
        GetController controller = controller(results, versionRepository(tempDir), channelRepository(tempDir), sent);

        PluginResult result = controller.download(record("MyPlugin"));

        assertEquals(Outcome.NETWORK_ERROR, result.getOutcome());
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).contains("network error"));
    }

    @Test
    void download_returnsFileErrorOutcomeAndSendsDiscordNotification(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.FILE_ERROR);
        List<String> sent = new ArrayList<>();
        GetController controller = controller(results, versionRepository(tempDir), channelRepository(tempDir), sent);

        PluginResult result = controller.download(record("MyPlugin"));

        assertEquals(Outcome.FILE_ERROR, result.getOutcome());
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).contains("file write error"));
    }

    @Test
    void download_returnsOtherFailureOutcomeForUnrecognizedNegativeResult(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", -99);
        List<String> sent = new ArrayList<>();
        GetController controller = controller(results, versionRepository(tempDir), channelRepository(tempDir), sent);

        PluginResult result = controller.download(record("MyPlugin"));

        assertEquals(Outcome.OTHER_FAILURE, result.getOutcome());
        assertTrue(sent.isEmpty(), "no Discord notification for unrecognized failures");
    }

    @Test
    void download_returnsDownloadedOutcomeWithBytesAndStoredTagOnSuccess(@TempDir Path tempDir) {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MyPlugin", "v2.0.0");
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", 4096);
        GetController controller = controller(results, versionRepository, channelRepository(tempDir), new ArrayList<>());

        PluginResult result = controller.download(record("MyPlugin"));

        assertEquals(Outcome.DOWNLOADED, result.getOutcome());
        assertEquals(4096, result.getDownloadedBytes());
        assertEquals("v2.0.0", result.getStoredTag());
    }

    // -------------------------------------------------------------------------
    // download() — release channels
    // -------------------------------------------------------------------------

    @Test
    void download_usesTheStoredChannelWhenNoChannelIsRequested(@TempDir Path tempDir) {
        ChannelRepository channelRepository = channelRepository(tempDir);
        channelRepository.setChannel("MyPlugin", ReleaseChannel.EXPERIMENTAL);
        List<ReleaseChannel> channelsUsed = new ArrayList<>();
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", 2048);
        GetController controller = new GetController(
                stubDownloadService(results, channelsUsed), new DependencyResolutionService(null, null),
                versionRepository(tempDir), channelRepository, recordingDiscordService(new ArrayList<>()),
                Logger.getLogger("GetControllerTest"));

        PluginResult result = controller.download(record("MyPlugin"));

        assertEquals(List.of(ReleaseChannel.EXPERIMENTAL), channelsUsed);
        assertEquals(ReleaseChannel.EXPERIMENTAL, result.getChannel());
    }

    @Test
    void download_pinsThePluginToExperimentalAfterASuccessfulDownload(@TempDir Path tempDir) {
        ChannelRepository channelRepository = channelRepository(tempDir);
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", 2048);
        GetController controller = controller(results, versionRepository(tempDir), channelRepository, new ArrayList<>());

        controller.download(record("MyPlugin"), ReleaseChannel.EXPERIMENTAL);

        assertEquals(ReleaseChannel.EXPERIMENTAL, channelRepository.getChannel("MyPlugin"));
    }

    @Test
    void download_pinsThePluginToExperimentalWhenItIsAlreadyUpToDate(@TempDir Path tempDir) {
        ChannelRepository channelRepository = channelRepository(tempDir);
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.ALREADY_UP_TO_DATE);
        GetController controller = controller(results, versionRepository(tempDir), channelRepository, new ArrayList<>());

        controller.download(record("MyPlugin"), ReleaseChannel.EXPERIMENTAL);

        assertEquals(ReleaseChannel.EXPERIMENTAL, channelRepository.getChannel("MyPlugin"));
    }

    @Test
    void download_stableRequestClearsAnExistingExperimentalPin(@TempDir Path tempDir) {
        ChannelRepository channelRepository = channelRepository(tempDir);
        channelRepository.setChannel("MyPlugin", ReleaseChannel.EXPERIMENTAL);
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", 2048);
        GetController controller = controller(results, versionRepository(tempDir), channelRepository, new ArrayList<>());

        controller.download(record("MyPlugin"), ReleaseChannel.STABLE);

        assertEquals(ReleaseChannel.STABLE, channelRepository.getChannel("MyPlugin"));
    }

    @Test
    void download_doesNotPinWhenTheRequestedChannelPublishesNoBuild(@TempDir Path tempDir) {
        // Pinning here would strand the plugin on a channel with nothing to install,
        // silently skipping it on every later /dpm update.
        ChannelRepository channelRepository = channelRepository(tempDir);
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.NO_RELEASE);
        GetController controller = controller(results, versionRepository(tempDir), channelRepository, new ArrayList<>());

        PluginResult result = controller.download(record("MyPlugin"), ReleaseChannel.EXPERIMENTAL);

        assertEquals(Outcome.NO_RELEASE, result.getOutcome());
        assertEquals(ReleaseChannel.EXPERIMENTAL, result.getChannel());
        assertEquals(ReleaseChannel.STABLE, channelRepository.getChannel("MyPlugin"));
    }

    @Test
    void download_doesNotPinWhenTheDownloadFails(@TempDir Path tempDir) {
        ChannelRepository channelRepository = channelRepository(tempDir);
        Map<String, Integer> results = new HashMap<>();
        results.put("MyPlugin", DownloadService.NETWORK_ERROR);
        GetController controller = controller(results, versionRepository(tempDir), channelRepository, new ArrayList<>());

        controller.download(record("MyPlugin"), ReleaseChannel.EXPERIMENTAL);

        assertEquals(ReleaseChannel.STABLE, channelRepository.getChannel("MyPlugin"));
    }

    // -------------------------------------------------------------------------
    // runTargets()
    // -------------------------------------------------------------------------

    @Test
    void runTargets_doesNotApplyARequestedChannelToDependencies(@TempDir Path tempDir) {
        ChannelRepository channelRepository = channelRepository(tempDir);
        List<ReleaseChannel> channelsUsed = new ArrayList<>();
        Map<String, Integer> results = new HashMap<>();
        results.put("Dependency", 1024);
        results.put("MyPlugin", 2048);
        GetController controller = new GetController(
                stubDownloadService(results, channelsUsed), new DependencyResolutionService(null, null),
                versionRepository(tempDir), channelRepository, recordingDiscordService(new ArrayList<>()),
                Logger.getLogger("GetControllerTest"));

        controller.runTargets(List.of(
                Target.usingStoredChannel(record("Dependency")),
                Target.of(record("MyPlugin"), ReleaseChannel.EXPERIMENTAL)));

        assertEquals(List.of(ReleaseChannel.STABLE, ReleaseChannel.EXPERIMENTAL), channelsUsed,
                "A dependency must keep its own channel rather than inheriting --experimental");
        assertEquals(ReleaseChannel.STABLE, channelRepository.getChannel("Dependency"));
        assertEquals(ReleaseChannel.EXPERIMENTAL, channelRepository.getChannel("MyPlugin"));
    }

    // -------------------------------------------------------------------------
    // runBatch()
    // -------------------------------------------------------------------------

    @Test
    void runBatch_returnsOneResultPerRecordInOrder(@TempDir Path tempDir) {
        Map<String, Integer> results = new HashMap<>();
        results.put("PluginA", DownloadService.ALREADY_UP_TO_DATE);
        results.put("PluginB", DownloadService.NO_RELEASE);
        GetController controller = controller(results, versionRepository(tempDir), channelRepository(tempDir), new ArrayList<>());

        List<PluginResult> batchResults = controller.runBatch(List.of(record("PluginA"), record("PluginB")));

        assertEquals(2, batchResults.size());
        assertEquals("PluginA", batchResults.get(0).getRecord().getName());
        assertEquals(Outcome.ALREADY_UP_TO_DATE, batchResults.get(0).getOutcome());
        assertEquals("PluginB", batchResults.get(1).getRecord().getName());
        assertEquals(Outcome.NO_RELEASE, batchResults.get(1).getOutcome());
    }
}
