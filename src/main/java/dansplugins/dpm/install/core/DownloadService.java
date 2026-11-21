package dansplugins.dpm.install.core;

import dansplugins.dpm.install.ports.DebugLog;
import dansplugins.dpm.install.ports.JarDownloader;
import dansplugins.dpm.install.ports.PluginFolder;
import dansplugins.dpm.install.ports.ReleaseSource;
import dansplugins.dpm.install.ports.VersionStore;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;

import java.util.List;

public class DownloadService {
    /** No published release found on GitHub for this plugin. */
    public static final int NO_RELEASE = -2;
    /** The installed JAR is present and its version matches the latest release. */
    public static final int ALREADY_UP_TO_DATE = -3;
    /** GitHub or the network was unreachable during the download attempt. */
    public static final int NETWORK_ERROR = JarDownloader.NETWORK_ERROR;
    /** The JAR was fetched but could not be written to the plugins folder. */
    public static final int FILE_ERROR = JarDownloader.FILE_ERROR;

    private final DebugLog logger;
    private final ReleaseSource gitHubReleaseRepository;
    private final PluginFolder pluginFileRepository;
    private final VersionStore versionRepository;
    private final JarDownloader jarDownloader;

    public DownloadService(DebugLog logger, ReleaseSource gitHubReleaseRepository,
                           PluginFolder pluginFileRepository, VersionStore versionRepository,
                           JarDownloader jarDownloader) {
        this.logger = logger;
        this.gitHubReleaseRepository = gitHubReleaseRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.versionRepository = versionRepository;
        this.jarDownloader = jarDownloader;
    }

    public int downloadLatest(ProjectRecord projectRecord) {
        return downloadLatest(projectRecord, ReleaseChannel.STABLE);
    }

    public int downloadLatest(ProjectRecord projectRecord, ReleaseChannel channel) {
        return downloadLatest(projectRecord, channel, pluginFileRepository.isInstalled(projectRecord));
    }

    // physicallyInstalled lets callers that have already confirmed the JAR is present (e.g. via
    // filterInstalled()) skip the per-call isInstalled() directory scan.
    public int downloadLatest(ProjectRecord projectRecord, boolean physicallyInstalled) {
        return downloadLatest(projectRecord, ReleaseChannel.STABLE, physicallyInstalled);
    }

    // A channel switch needs no special handling: the stored tag from one channel never matches the
    // other channel's tag, so the up-to-date check falls through and the JAR is re-downloaded.
    public int downloadLatest(ProjectRecord projectRecord, ReleaseChannel channel, boolean physicallyInstalled) {
        ReleaseInfo release = gitHubReleaseRepository.getRelease(projectRecord.getOwner(), projectRecord.getRepo(), channel);
        if (release == ReleaseInfo.NO_RELEASE) {
            return NO_RELEASE;
        }
        if (release == null) {
            logger.warn("Could not resolve release info for " + projectRecord.getName() + ".");
            return NETWORK_ERROR;
        }

        String latestTag = release.getTagName();
        if (latestTag != null
                && latestTag.equals(versionRepository.getStoredTag(projectRecord.getName()))
                && physicallyInstalled) {
            return ALREADY_UP_TO_DATE;
        }

        String dest = pluginFileRepository.managedJarPath(projectRecord);
        int bytes = jarDownloader.download(release.getJarUrl(), dest);
        if (bytes > 0) {
            removeConflictingJars(projectRecord);
            if (latestTag != null) {
                versionRepository.setTag(projectRecord.getName(), latestTag);
            }
        }
        return bytes;
    }

    private void removeConflictingJars(ProjectRecord projectRecord) {
        List<String> conflicts = pluginFileRepository.conflictingJars(projectRecord);
        for (String conflict : conflicts) {
            if (pluginFileRepository.deleteJar(conflict)) {
                logger.log("Removed older JAR after download: " + conflict);
            } else {
                logger.log("Failed to remove older JAR: " + conflict);
            }
        }
    }
}
