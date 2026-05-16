package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;
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

    private static final String PATH_TO_PLUGINS_FOLDER = "./plugins/";

    private final Logger logger;
    private final GitHubReleaseService gitHubReleaseService;
    private final PluginFolderService pluginFolderService;

    public DownloadService(Logger logger, GitHubReleaseService gitHubReleaseService, PluginFolderService pluginFolderService) {
        this.logger = logger;
        this.gitHubReleaseService = gitHubReleaseService;
        this.pluginFolderService = pluginFolderService;
    }

    /**
     * Resolves the latest JAR URL via the GitHub API, removes any conflicting
     * JARs already in the plugins folder, then downloads the new version.
     * Returns bytes downloaded on success, {@link #NO_RELEASE} if no release
     * has been published yet, or -1 on other errors.
     */
    public int downloadLatest(ProjectRecord projectRecord) {
        String downloadUrl = gitHubReleaseService.getLatestJarDownloadUrl(projectRecord.getOwner(), projectRecord.getRepo());
        if (GitHubReleaseService.NO_RELEASE.equals(downloadUrl)) {
            return NO_RELEASE;
        }
        if (downloadUrl == null) {
            logger.log("Could not resolve a download URL for " + projectRecord.getName() + ".");
            return -1;
        }
        removeConflictingJars(projectRecord);
        return downloadFromUrl(downloadUrl, PATH_TO_PLUGINS_FOLDER + projectRecord.getName() + ".jar");
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
