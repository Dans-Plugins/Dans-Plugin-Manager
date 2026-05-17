package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.utils.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class DownloadService {
    /** No published release found on GitHub for this plugin. */
    public static final int NO_RELEASE = -2;
    /** The installed JAR is present and its version matches the latest release. */
    public static final int ALREADY_UP_TO_DATE = -3;

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
        ReleaseInfo release = gitHubReleaseService.getLatestRelease(projectRecord.getOwner(), projectRecord.getRepo());
        if (release == ReleaseInfo.NO_RELEASE) {
            return NO_RELEASE;
        }
        if (release == null) {
            logger.log("Could not resolve release info for " + projectRecord.getName() + ".");
            return -1;
        }

        String latestTag = release.getTagName();
        if (latestTag != null
                && latestTag.equals(versionStore.getStoredTag(projectRecord.getName()))
                && pluginFolderService.isInstalled(projectRecord)) {
            return ALREADY_UP_TO_DATE;
        }

        removeConflictingJars(projectRecord);
        String dest = pluginFolderService.getPluginsFolder() + projectRecord.getName() + ".jar";
        int bytes = downloadFromUrl(release.getJarUrl(), dest);
        if (bytes > 0 && latestTag != null) {
            versionStore.setTag(projectRecord.getName(), latestTag);
        }
        return bytes;
    }

    private void removeConflictingJars(ProjectRecord projectRecord) {
        List<File> conflicts = pluginFolderService.findConflictingJars(projectRecord);
        for (File conflict : conflicts) {
            if (conflict.delete()) {
                logger.log("Removed conflicting JAR before download: " + conflict.getName());
            } else {
                logger.log("Failed to remove conflicting JAR: " + conflict.getName());
            }
        }
    }

    private int downloadFromUrl(String url, String path) {
        try {
            return readAndWrite(url, path);
        } catch (IOException e) {
            logger.log("Something went wrong downloading from " + url + ": " + e.getMessage());
            return -1;
        }
    }

    int readAndWrite(String link, String path) throws IOException {
        File dest = new File(path);
        try (BufferedInputStream in = new BufferedInputStream(new URL(link).openStream());
             FileOutputStream out = new FileOutputStream(dest)) {
            int totalBytes = 0;
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(data, 0, 1024)) != -1) {
                totalBytes += bytesRead;
                out.write(data, 0, bytesRead);
            }
            return totalBytes;
        } catch (IOException e) {
            dest.delete();
            throw e;
        }
    }
}
