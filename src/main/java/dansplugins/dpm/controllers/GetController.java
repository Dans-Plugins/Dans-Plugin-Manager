package dansplugins.dpm.controllers;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.repositories.ChannelRepository;
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
        private final ReleaseChannel channel;

        PluginResult(ProjectRecord record, Outcome outcome, int downloadedBytes, String storedTag, ReleaseChannel channel) {
            this.record = record;
            this.outcome = outcome;
            this.downloadedBytes = downloadedBytes;
            this.storedTag = storedTag;
            this.channel = channel;
        }

        public ProjectRecord getRecord() { return record; }
        public Outcome getOutcome() { return outcome; }
        public int getDownloadedBytes() { return downloadedBytes; }
        public String getStoredTag() { return storedTag; }
        /** The channel this download was attempted on. */
        public ReleaseChannel getChannel() { return channel; }
    }

    /**
     * A plugin to download, plus the channel the sender explicitly asked for.
     *
     * <p>A null {@code requestedChannel} means "whatever this plugin is already pinned to" —
     * that is what dependencies pulled in automatically use, so an {@code --experimental} flag on
     * one plugin never drags its dependencies onto the experimental channel too.
     */
    public static final class Target {
        private final ProjectRecord record;
        private final ReleaseChannel requestedChannel;

        private Target(ProjectRecord record, ReleaseChannel requestedChannel) {
            this.record = record;
            this.requestedChannel = requestedChannel;
        }

        public static Target of(ProjectRecord record, ReleaseChannel requestedChannel) {
            return new Target(record, requestedChannel);
        }

        public static Target usingStoredChannel(ProjectRecord record) {
            return new Target(record, null);
        }

        public ProjectRecord getRecord() { return record; }
        public ReleaseChannel getRequestedChannel() { return requestedChannel; }
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
    private final ChannelRepository channelRepository;
    private final DiscordNotificationService discordNotificationService;
    private final Logger logger;

    public GetController(DownloadService downloadService, DependencyResolutionService dependencyResolutionService,
                         VersionRepository versionRepository, ChannelRepository channelRepository,
                         DiscordNotificationService discordNotificationService, Logger logger) {
        this.downloadService = downloadService;
        this.dependencyResolutionService = dependencyResolutionService;
        this.versionRepository = versionRepository;
        this.channelRepository = channelRepository;
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
        List<Target> targets = new ArrayList<>();
        for (ProjectRecord record : records) {
            targets.add(Target.usingStoredChannel(record));
        }
        return runTargets(targets);
    }

    public List<PluginResult> runTargets(List<Target> targets) {
        List<PluginResult> results = new ArrayList<>();
        for (Target target : targets) {
            results.add(download(target.getRecord(), target.getRequestedChannel()));
        }
        return results;
    }

    public PluginResult download(ProjectRecord record) {
        return download(record, null);
    }

    /**
     * Downloads one plugin. A null {@code requestedChannel} uses the plugin's stored channel;
     * otherwise the requested channel is used, and pinned to the plugin if the download works out.
     */
    public PluginResult download(ProjectRecord record, ReleaseChannel requestedChannel) {
        ReleaseChannel channel = requestedChannel != null
                ? requestedChannel
                : channelRepository.getChannel(record.getName());

        int result = downloadService.downloadLatest(record, channel);
        if (result == DownloadService.NO_RELEASE) {
            return new PluginResult(record, Outcome.NO_RELEASE, 0, null, channel);
        }
        if (result == DownloadService.ALREADY_UP_TO_DATE) {
            pinChannel(record, requestedChannel);
            return new PluginResult(record, Outcome.ALREADY_UP_TO_DATE, 0, versionRepository.getStoredTag(record.getName()), channel);
        }
        if (result == DownloadService.NETWORK_ERROR) {
            logger.warning("[DPM] Failed to install " + record.getName() + " — could not reach GitHub.");
            discordNotificationService.send("[DPM] Failed to install " + record.getName() + ": network error");
            return new PluginResult(record, Outcome.NETWORK_ERROR, 0, null, channel);
        }
        if (result == DownloadService.FILE_ERROR) {
            logger.warning("[DPM] Failed to install " + record.getName() + " — could not write to plugins folder.");
            discordNotificationService.send("[DPM] Failed to install " + record.getName() + ": file write error");
            return new PluginResult(record, Outcome.FILE_ERROR, 0, null, channel);
        }
        if (result < 0) {
            logger.warning("[DPM] Failed to install " + record.getName() + ".");
            return new PluginResult(record, Outcome.OTHER_FAILURE, 0, null, channel);
        }
        pinChannel(record, requestedChannel);
        String tag = versionRepository.getStoredTag(record.getName());
        logger.info("[DPM] Installed " + record.getName() + (tag != null ? " " + tag : "")
                + (channel == ReleaseChannel.EXPERIMENTAL ? " (experimental)" : "") + ".");
        return new PluginResult(record, Outcome.DOWNLOADED, result, tag, channel);
    }

    // Only an explicit request repins the plugin, and only once the channel has proven to have a
    // build — pinning on a failed or unpublished channel would strand the plugin there, silently
    // skipping it on every later /dpm update.
    private void pinChannel(ProjectRecord record, ReleaseChannel requestedChannel) {
        if (requestedChannel == null) return;
        if (requestedChannel == ReleaseChannel.EXPERIMENTAL) {
            channelRepository.setChannel(record.getName(), ReleaseChannel.EXPERIMENTAL);
        } else {
            // STABLE is the default, so returning to it means dropping the entry entirely.
            channelRepository.removeChannel(record.getName());
        }
    }
}
