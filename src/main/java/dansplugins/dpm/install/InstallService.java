package dansplugins.dpm.install;

import dansplugins.dpm.install.adapters.HttpJarDownloader;
import dansplugins.dpm.install.core.DependencyResolutionService;
import dansplugins.dpm.install.core.DownloadService;
import dansplugins.dpm.install.core.GetController;
import dansplugins.dpm.install.core.UpdateController;
import dansplugins.dpm.install.ports.ChannelStore;
import dansplugins.dpm.install.ports.DebugLog;
import dansplugins.dpm.install.ports.JarDownloader;
import dansplugins.dpm.install.ports.Notifier;
import dansplugins.dpm.install.ports.PluginFolder;
import dansplugins.dpm.install.ports.ReleaseSource;
import dansplugins.dpm.install.ports.VersionStore;
import dansplugins.dpm.repositories.ProjectRecordRepository;

import java.util.logging.Logger;

/**
 * Composition root for the install pipeline: builds the core ({@link DownloadService},
 * {@link GetController}, {@link UpdateController}) from the ports it is given.
 *
 * <p>The release source, plugins folder, stores and notifier are passed in because the rest of the
 * plugin (list, info, remove, reload) shares the same instances; the release cache in particular
 * must be the one {@code /dpm reload} clears. The jar downloader is chosen here: HTTP by default.
 */
public final class InstallService {
    private final DownloadService downloadService;
    private final GetController getController;
    private final UpdateController updateController;

    public InstallService(ProjectRecordRepository projectRecords, ReleaseSource releases, PluginFolder pluginFolder,
                          VersionStore versions, ChannelStore channels, Notifier notifier,
                          DependencyResolutionService dependencyResolution, DebugLog debugLog, Logger consoleLog) {
        this(projectRecords, releases, pluginFolder, versions, channels, notifier, dependencyResolution, debugLog,
                consoleLog, new HttpJarDownloader(debugLog));
    }

    public InstallService(ProjectRecordRepository projectRecords, ReleaseSource releases, PluginFolder pluginFolder,
                          VersionStore versions, ChannelStore channels, Notifier notifier,
                          DependencyResolutionService dependencyResolution, DebugLog debugLog, Logger consoleLog,
                          JarDownloader jarDownloader) {
        this.downloadService = new DownloadService(debugLog, releases, pluginFolder, versions, jarDownloader);
        this.getController = new GetController(downloadService, dependencyResolution, versions, channels, notifier, consoleLog);
        this.updateController = new UpdateController(projectRecords, pluginFolder, downloadService, versions, channels,
                notifier, consoleLog);
    }

    public DownloadService getDownloadService() { return downloadService; }
    public GetController getGetController() { return getController; }
    public UpdateController getUpdateController() { return updateController; }
}
