package dansplugins.dpm.controllers;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.services.DiscordNotificationService;
import dansplugins.dpm.services.DownloadService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// Orchestrates /dpm update: the installed-plugin scan, selective-update validation, and the
// download loop, returning plain result data rather than sending messages. UpdateCommand
// formats and sends the results.
public class UpdateController {

    public enum Outcome { UPDATED, ALREADY_UP_TO_DATE, NO_RELEASE, NETWORK_ERROR, FILE_ERROR, OTHER_FAILURE }

    public static final class PluginResult {
        private final ProjectRecord record;
        private final Outcome outcome;
        private final String oldTag;
        private final String newTag;

        PluginResult(ProjectRecord record, Outcome outcome, String oldTag, String newTag) {
            this.record = record;
            this.outcome = outcome;
            this.oldTag = oldTag;
            this.newTag = newTag;
        }

        public ProjectRecord getRecord() { return record; }
        public Outcome getOutcome() { return outcome; }
        public String getOldTag() { return oldTag; }
        public String getNewTag() { return newTag; }
    }

    public static final class SelectionResult {
        private final List<ProjectRecord> toUpdate;
        private final List<String> notFound;
        private final List<String> notInstalled;

        SelectionResult(List<ProjectRecord> toUpdate, List<String> notFound, List<String> notInstalled) {
            this.toUpdate = toUpdate;
            this.notFound = notFound;
            this.notInstalled = notInstalled;
        }

        public List<ProjectRecord> getToUpdate() { return toUpdate; }
        public List<String> getNotFound() { return notFound; }
        public List<String> getNotInstalled() { return notInstalled; }
    }

    private final ProjectRecordRepository projectRecordRepository;
    private final PluginFileRepository pluginFileRepository;
    private final DownloadService downloadService;
    private final VersionRepository versionRepository;
    private final ChannelRepository channelRepository;
    private final DiscordNotificationService discordNotificationService;
    private final Logger logger;

    public UpdateController(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository,
                            DownloadService downloadService, VersionRepository versionRepository,
                            ChannelRepository channelRepository, DiscordNotificationService discordNotificationService,
                            Logger logger) {
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.downloadService = downloadService;
        this.versionRepository = versionRepository;
        this.channelRepository = channelRepository;
        this.discordNotificationService = discordNotificationService;
        this.logger = logger;
    }

    public List<ProjectRecord> getInstalledPlugins() {
        return pluginFileRepository.filterInstalled(projectRecordRepository.getAllProjectRecords());
    }

    public SelectionResult selectForUpdate(String[] names) {
        List<ProjectRecord> candidates = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        for (String name : names) {
            ProjectRecord record = projectRecordRepository.getProjectRecord(name);
            if (record == null) {
                notFound.add(name);
            } else {
                candidates.add(record);
            }
        }
        Set<String> installedNames = pluginFileRepository.filterInstalled(candidates)
                .stream().map(ProjectRecord::getName).collect(Collectors.toSet());
        List<ProjectRecord> toUpdate = new ArrayList<>();
        List<String> notInstalled = new ArrayList<>();
        for (ProjectRecord record : candidates) {
            if (installedNames.contains(record.getName())) {
                toUpdate.add(record);
            } else {
                notInstalled.add(record.getName());
            }
        }
        return new SelectionResult(toUpdate, notFound, notInstalled);
    }

    public List<PluginResult> runBatch(List<ProjectRecord> records) {
        List<PluginResult> results = new ArrayList<>();
        for (ProjectRecord record : records) {
            results.add(update(record));
        }
        sendUpdateNotification(results);
        return results;
    }

    // /dpm update takes no channel flags — each plugin is updated on whatever channel it is pinned to.
    private PluginResult update(ProjectRecord record) {
        String oldTag = versionRepository.getStoredTag(record.getName());
        ReleaseChannel channel = channelRepository.getChannel(record.getName());
        int result = downloadService.downloadLatest(record, channel, true);
        if (result == DownloadService.ALREADY_UP_TO_DATE) {
            return new PluginResult(record, Outcome.ALREADY_UP_TO_DATE, oldTag, oldTag);
        }
        if (result == DownloadService.NO_RELEASE) {
            return new PluginResult(record, Outcome.NO_RELEASE, oldTag, null);
        }
        if (result > 0) {
            String newTag = versionRepository.getStoredTag(record.getName());
            String versionDiff = versionDiffSuffix(oldTag, newTag);
            logger.info("[DPM] Updated " + record.getName() + versionDiff + ".");
            return new PluginResult(record, Outcome.UPDATED, oldTag, newTag);
        }
        if (result == DownloadService.NETWORK_ERROR) {
            logger.warning("[DPM] Failed to update " + record.getName() + " — could not reach GitHub.");
            return new PluginResult(record, Outcome.NETWORK_ERROR, oldTag, null);
        }
        if (result == DownloadService.FILE_ERROR) {
            logger.warning("[DPM] Failed to update " + record.getName() + " — could not write to plugins folder.");
            return new PluginResult(record, Outcome.FILE_ERROR, oldTag, null);
        }
        logger.warning("[DPM] Failed to update " + record.getName() + ".");
        return new PluginResult(record, Outcome.OTHER_FAILURE, oldTag, null);
    }

    private void sendUpdateNotification(List<PluginResult> results) {
        int updated = 0, upToDate = 0, skipped = 0, failed = 0;
        List<String> versionDiffs = new ArrayList<>();
        for (PluginResult result : results) {
            switch (result.getOutcome()) {
                case UPDATED:
                    updated++;
                    versionDiffs.add(result.getRecord().getName() + versionDiffSuffix(result.getOldTag(), result.getNewTag()));
                    break;
                case ALREADY_UP_TO_DATE:
                    upToDate++;
                    break;
                case NO_RELEASE:
                    skipped++;
                    break;
                default:
                    failed++;
                    break;
            }
        }
        StringBuilder msg = new StringBuilder("[DPM] Update complete: ")
                .append(updated).append(" updated, ").append(upToDate).append(" already up to date");
        if (skipped > 0) msg.append(", ").append(skipped).append(" skipped");
        if (failed > 0) msg.append(", ").append(failed).append(" failed");
        for (String diff : versionDiffs) msg.append("\n• ").append(diff);
        discordNotificationService.send(msg.toString());
    }

    public static String versionDiffSuffix(String oldTag, String newTag) {
        if (oldTag != null && newTag != null) return " " + oldTag + " → " + newTag;
        if (newTag != null) return " " + newTag;
        return "";
    }
}
