package dansplugins.dpm.controllers;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Performs the /dpm list installed/version lookups and the /dpm list outdated staleness check, returning plain result data rather than
// sending messages. ListCommand formats and sends the results.
public class ListController {

    public static final class ListEntry {
        private final ProjectRecord record;
        private final boolean installed;
        private final String storedTag;

        ListEntry(ProjectRecord record, boolean installed, String storedTag) {
            this.record = record;
            this.installed = installed;
            this.storedTag = storedTag;
        }

        public ProjectRecord getRecord() { return record; }
        public boolean isInstalled() { return installed; }
        public String getStoredTag() { return storedTag; }
    }

    public enum Staleness { OUTDATED, UP_TO_DATE, NO_RELEASE, LOOKUP_FAILED }

    public static final class OutdatedEntry {
        private final ProjectRecord record;
        private final Staleness staleness;
        private final String storedTag;
        private final String latestTag;
        private final ReleaseChannel channel;

        OutdatedEntry(ProjectRecord record, Staleness staleness, String storedTag, String latestTag, ReleaseChannel channel) {
            this.record = record;
            this.staleness = staleness;
            this.storedTag = storedTag;
            this.latestTag = latestTag;
            this.channel = channel;
        }

        public ProjectRecord getRecord() { return record; }
        public Staleness getStaleness() { return staleness; }
        public String getStoredTag() { return storedTag; }
        /** The newest build on this plugin's channel, or null when none could be resolved. */
        public String getLatestTag() { return latestTag; }
        public ReleaseChannel getChannel() { return channel; }
    }

    private final ProjectRecordRepository projectRecordRepository;
    private final PluginFileRepository pluginFileRepository;
    private final VersionRepository versionRepository;
    private final GitHubReleaseRepository gitHubReleaseRepository;
    private final ChannelRepository channelRepository;

    public ListController(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository,
                          VersionRepository versionRepository, GitHubReleaseRepository gitHubReleaseRepository,
                          ChannelRepository channelRepository) {
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.versionRepository = versionRepository;
        this.gitHubReleaseRepository = gitHubReleaseRepository;
        this.channelRepository = channelRepository;
    }

    public List<ListEntry> listAll() {
        List<ProjectRecord> records = projectRecordRepository.getAllProjectRecords();
        Set<String> installedNames = installedNames(records);
        List<ListEntry> entries = new ArrayList<>();
        for (ProjectRecord record : records) {
            entries.add(toEntry(record, installedNames.contains(record.getName())));
        }
        return entries;
    }

    public List<ListEntry> listInstalled() {
        List<ProjectRecord> installed = pluginFileRepository.filterInstalled(projectRecordRepository.getAllProjectRecords());
        List<ListEntry> entries = new ArrayList<>();
        for (ProjectRecord record : installed) {
            entries.add(toEntry(record, true));
        }
        return entries;
    }

    // Available plugins are by definition not installed and carry no stored tag, so the records
    // themselves are returned rather than entries with two dead fields.
    public List<ProjectRecord> listAvailable() {
        List<ProjectRecord> records = projectRecordRepository.getAllProjectRecords();
        Set<String> installedNames = installedNames(records);
        List<ProjectRecord> available = new ArrayList<>();
        for (ProjectRecord record : records) {
            if (!installedNames.contains(record.getName())) available.add(record);
        }
        return available;
    }

    // Touches the GitHub API — callers must invoke this off the main thread. Uses the same
    // comparison /dpm update makes before downloading (stored tag vs. the newest build on the
    // plugin's pinned channel), but never downloads anything.
    public List<OutdatedEntry> listOutdated() {
        List<ProjectRecord> installed = pluginFileRepository.filterInstalled(projectRecordRepository.getAllProjectRecords());
        List<OutdatedEntry> entries = new ArrayList<>();
        for (ProjectRecord record : installed) {
            entries.add(checkStaleness(record));
        }
        return entries;
    }

    private OutdatedEntry checkStaleness(ProjectRecord record) {
        ReleaseChannel channel = channelRepository.getChannel(record.getName());
        String storedTag = versionRepository.getStoredTag(record.getName());
        ReleaseInfo release = gitHubReleaseRepository.getReleaseMetadata(record.getOwner(), record.getRepo(), channel);
        if (release == ReleaseInfo.NO_RELEASE) {
            return new OutdatedEntry(record, Staleness.NO_RELEASE, storedTag, null, channel);
        }
        if (release == null) {
            return new OutdatedEntry(record, Staleness.LOOKUP_FAILED, storedTag, null, channel);
        }
        String latestTag = release.getTagName();
        Staleness staleness = latestTag != null && latestTag.equals(storedTag) ? Staleness.UP_TO_DATE : Staleness.OUTDATED;
        return new OutdatedEntry(record, staleness, storedTag, latestTag, channel);
    }

    private ListEntry toEntry(ProjectRecord record, boolean installed) {
        String storedTag = installed ? versionRepository.getStoredTag(record.getName()) : null;
        return new ListEntry(record, installed, storedTag);
    }

    private Set<String> installedNames(List<ProjectRecord> records) {
        Set<String> names = new HashSet<>();
        for (ProjectRecord record : pluginFileRepository.filterInstalled(records)) {
            names.add(record.getName());
        }
        return names;
    }
}
