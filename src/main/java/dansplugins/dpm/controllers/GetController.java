package dansplugins.dpm.controllers;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.services.DependencyResolutionService;
import dansplugins.dpm.services.DiscordNotificationService;
import dansplugins.dpm.services.DownloadService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// Orchestrates /dpm get: dependency resolution and the download loop, returning plain
// result data rather than sending messages. GetCommand formats and sends the results.
public class GetController {

    public enum Outcome { NO_RELEASE, ALREADY_UP_TO_DATE, NETWORK_ERROR, FILE_ERROR, OTHER_FAILURE, DOWNLOADED }

    public static final class PluginResult {
        private final ProjectRecord record;
        private final Outcome outcome;
        private final int downloadedBytes;
        private final String storedTag;

        PluginResult(ProjectRecord record, Outcome outcome, int downloadedBytes, String storedTag) {
            this.record = record;
            this.outcome = outcome;
            this.downloadedBytes = downloadedBytes;
            this.storedTag = storedTag;
        }

        public ProjectRecord getRecord() { return record; }
        public Outcome getOutcome() { return outcome; }
        public int getDownloadedBytes() { return downloadedBytes; }
        public String getStoredTag() { return storedTag; }
    }

    public static final class DependencyResolutionResult {
        private final List<ProjectRecord> depsToFetch;
        private final List<String> unknownDeps;

        DependencyResolutionResult(List<ProjectRecord> depsToFetch, List<String> unknownDeps) {
            this.depsToFetch = depsToFetch;
            this.unknownDeps = unknownDeps;
        }

        public List<ProjectRecord> getDepsToFetch() { return depsToFetch; }
        public List<String> getUnknownDeps() { return unknownDeps; }
    }

    private final DownloadService downloadService;
    private final DependencyResolutionService dependencyResolutionService;
    private final VersionRepository versionRepository;
    private final DiscordNotificationService discordNotificationService;
    private final Logger logger;

    public GetController(DownloadService downloadService, DependencyResolutionService dependencyResolutionService,
                         VersionRepository versionRepository, DiscordNotificationService discordNotificationService,
                         Logger logger) {
        this.downloadService = downloadService;
        this.dependencyResolutionService = dependencyResolutionService;
        this.versionRepository = versionRepository;
        this.discordNotificationService = discordNotificationService;
        this.logger = logger;
    }

    public DependencyResolutionResult resolveDependencies(List<ProjectRecord> records) {
        Set<String> resolved = records.stream()
                .map(r -> r.getName().toLowerCase())
                .collect(Collectors.toCollection(HashSet::new));
        List<ProjectRecord> depsToFetch = new ArrayList<>();
        List<String> unknownDeps = new ArrayList<>();
        dependencyResolutionService.resolve(records, resolved, depsToFetch, unknownDeps);
        return new DependencyResolutionResult(depsToFetch, unknownDeps);
    }

    public List<PluginResult> runBatch(List<ProjectRecord> records) {
        List<PluginResult> results = new ArrayList<>();
        for (ProjectRecord record : records) {
            results.add(download(record));
        }
        return results;
    }

    public PluginResult download(ProjectRecord record) {
        int result = downloadService.downloadLatest(record);
        if (result == DownloadService.NO_RELEASE) {
            return new PluginResult(record, Outcome.NO_RELEASE, 0, null);
        }
        if (result == DownloadService.ALREADY_UP_TO_DATE) {
            return new PluginResult(record, Outcome.ALREADY_UP_TO_DATE, 0, versionRepository.getStoredTag(record.getName()));
        }
        if (result == DownloadService.NETWORK_ERROR) {
            logger.warning("[DPM] Failed to install " + record.getName() + " — could not reach GitHub.");
            discordNotificationService.send("[DPM] Failed to install " + record.getName() + ": network error");
            return new PluginResult(record, Outcome.NETWORK_ERROR, 0, null);
        }
        if (result == DownloadService.FILE_ERROR) {
            logger.warning("[DPM] Failed to install " + record.getName() + " — could not write to plugins folder.");
            discordNotificationService.send("[DPM] Failed to install " + record.getName() + ": file write error");
            return new PluginResult(record, Outcome.FILE_ERROR, 0, null);
        }
        if (result < 0) {
            logger.warning("[DPM] Failed to install " + record.getName() + ".");
            return new PluginResult(record, Outcome.OTHER_FAILURE, 0, null);
        }
        String tag = versionRepository.getStoredTag(record.getName());
        logger.info("[DPM] Installed " + record.getName() + (tag != null ? " " + tag : "") + ".");
        return new PluginResult(record, Outcome.DOWNLOADED, result, tag);
    }
}
