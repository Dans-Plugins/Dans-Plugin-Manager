package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.utils.Logger;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

public class DownloadService {
    /** No published release found on GitHub for this plugin. */
    public static final int NO_RELEASE = -2;

    private static final String PATH_TO_PLUGINS_FOLDER = "./plugins/";

    private final Logger logger;
    private final GitHubReleaseService gitHubReleaseService;

    public DownloadService(Logger logger, GitHubReleaseService gitHubReleaseService) {
        this.logger = logger;
        this.gitHubReleaseService = gitHubReleaseService;
    }

    /**
     * Resolves the latest JAR URL via the GitHub API and downloads it.
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
        return downloadFromUrl(downloadUrl, PATH_TO_PLUGINS_FOLDER + projectRecord.getName() + ".jar");
    }

    private int downloadFromUrl(String url, String path) {
        try {
            return readAndWrite(url, path);
        } catch (IOException e) {
            logger.log("Something went wrong downloading from " + url + ": " + e.getMessage());
            return -1;
        }
    }

    private int readAndWrite(String link, String path) throws IOException {
        int totalBytes = 0;
        BufferedInputStream inputStream = new BufferedInputStream(new URL(link).openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(path);
        byte[] data = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, 1024)) != -1) {
            totalBytes += bytesRead;
            fileOutputStream.write(data, 0, bytesRead);
        }
        fileOutputStream.close();
        return totalBytes;
    }
}
