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
     * Resolves the download URL (querying GitHub if needed) and downloads the JAR.
     * Returns the number of chunks read on success, {@link #NO_RELEASE} if no release
     * has been published yet, or -1 on other errors.
     */
    public int downloadLatest(ProjectRecord projectRecord) {
        String downloadUrl = resolveDownloadUrl(projectRecord);
        if (GitHubReleaseService.NO_RELEASE.equals(downloadUrl)) {
            return NO_RELEASE;
        }
        if (downloadUrl == null) {
            logger.log("Could not resolve a download URL for " + projectRecord.getName() + ".");
            return -1;
        }
        return downloadFromUrl(downloadUrl, PATH_TO_PLUGINS_FOLDER + projectRecord.getName() + ".jar");
    }

    public int downloadFromUrl(String url, String path) {
        try {
            return readAndWrite(url, path);
        } catch (IOException e) {
            logger.log("Something went wrong downloading from " + url + ": " + e.getMessage());
            return -1;
        }
    }

    private String resolveDownloadUrl(ProjectRecord projectRecord) {
        if (projectRecord.isGitHubHosted()) {
            return gitHubReleaseService.getLatestJarDownloadUrl(projectRecord.getOwner(), projectRecord.getRepo());
        }
        return projectRecord.getDirectLink();
    }

    private int readAndWrite(String link, String path) throws IOException {
        int bytesRead = 0;
        BufferedInputStream inputStream = new BufferedInputStream(new URL(link).openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(path);
        byte[] data = new byte[1024];
        int byteContent;
        while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
            bytesRead++;
            fileOutputStream.write(data, 0, byteContent);
        }
        fileOutputStream.close();
        return bytesRead;
    }
}
