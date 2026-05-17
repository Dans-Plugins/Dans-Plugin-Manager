package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.utils.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DownloadService {
    /** No published release found on GitHub for this plugin. */
    public static final int NO_RELEASE = -2;
    /** The installed JAR is present and its version matches the latest release. */
    public static final int ALREADY_UP_TO_DATE = -3;
    /** GitHub or the network was unreachable during the download attempt. */
    public static final int NETWORK_ERROR = -4;
    /** The JAR was fetched but could not be written to the plugins folder. */
    public static final int FILE_ERROR = -5;

    private final Logger logger;
    private final GitHubReleaseService gitHubReleaseService;
    private final PluginFolderService pluginFolderService;
    private final VersionStore versionStore;

    public DownloadService(Logger logger, GitHubReleaseService gitHubReleaseService,
                           PluginFolderService pluginFolderService, VersionStore versionStore) {
        this.logger = logger;
        this.gitHubReleaseService = gitHubReleaseService;
        this.pluginFolderService = pluginFolderService;
        this.versionStore = versionStore;
    }

    public int downloadLatest(ProjectRecord projectRecord) {
        return downloadLatest(projectRecord, pluginFolderService.isInstalled(projectRecord));
    }

    // physicallyInstalled lets callers that have already confirmed the JAR is present (e.g. via
    // filterInstalled()) skip the per-call isInstalled() directory scan.
    public int downloadLatest(ProjectRecord projectRecord, boolean physicallyInstalled) {
        ReleaseInfo release = gitHubReleaseService.getLatestRelease(projectRecord.getOwner(), projectRecord.getRepo());
        if (release == ReleaseInfo.NO_RELEASE) {
            return NO_RELEASE;
        }
        if (release == null) {
            logger.warn("Could not resolve release info for " + projectRecord.getName() + ".");
            return NETWORK_ERROR;
        }

        String latestTag = release.getTagName();
        if (latestTag != null
                && latestTag.equals(versionStore.getStoredTag(projectRecord.getName()))
                && physicallyInstalled) {
            return ALREADY_UP_TO_DATE;
        }

        String dest = pluginFolderService.getPluginsFolder() + projectRecord.getName() + ".jar";
        int bytes = downloadFromUrl(release.getJarUrl(), dest);
        if (bytes > 0) {
            removeConflictingJars(projectRecord);
            if (latestTag != null) {
                versionStore.setTag(projectRecord.getName(), latestTag);
            }
        }
        return bytes;
    }

    private void removeConflictingJars(ProjectRecord projectRecord) {
        List<File> conflicts = pluginFolderService.findConflictingJars(projectRecord);
        for (File conflict : conflicts) {
            if (conflict.delete()) {
                logger.log("Removed older JAR after download: " + conflict.getName());
            } else {
                logger.log("Failed to remove older JAR: " + conflict.getName());
            }
        }
    }

    private int downloadFromUrl(String url, String path) {
        BufferedInputStream stream;
        try {
            stream = openNetworkStream(url);
        } catch (IOException e) {
            logger.warn("Network error reaching " + url + ": " + e.getMessage());
            return NETWORK_ERROR;
        }
        File dest = new File(path);
        try {
            return writeStreamToFile(stream, dest);
        } catch (IOException e) {
            dest.delete();
            logger.warn("File write error to " + path + ": " + e.getMessage());
            return FILE_ERROR;
        }
    }

    BufferedInputStream openNetworkStream(String url) throws IOException {
        IOException lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (attempt > 0) sleepMs(2000);
            try {
                return doOpenNetworkStream(url);
            } catch (IOException e) {
                lastError = e;
            }
        }
        throw lastError;
    }

    // package-private so tests can override via anonymous subclass
    BufferedInputStream doOpenNetworkStream(String url) throws IOException {
        URL u = new URL(url);
        String protocol = u.getProtocol();
        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            return new BufferedInputStream(connection.getInputStream());
        }
        return new BufferedInputStream(u.openStream());
    }

    void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int writeStreamToFile(BufferedInputStream in, File dest) throws IOException {
        try (in; FileOutputStream out = new FileOutputStream(dest)) {
            int totalBytes = 0;
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(data)) != -1) {
                totalBytes += bytesRead;
                out.write(data, 0, bytesRead);
            }
            return totalBytes;
        }
    }

    // Kept for direct test use — calls openNetworkStream then writeStreamToFile.
    int readAndWrite(String link, String path) throws IOException {
        File dest = new File(path);
        try {
            BufferedInputStream in = openNetworkStream(link);
            return writeStreamToFile(in, dest);
        } catch (IOException e) {
            dest.delete();
            throw e;
        }
    }
}
