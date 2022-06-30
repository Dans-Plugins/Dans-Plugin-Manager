package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.utils.Logger;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

public class DownloadService {
    private final Logger logger;

    private final static String PATH_TO_PLUGINS_FOLDER = "./plugins/";

    public DownloadService(Logger logger) {
        this.logger = logger;
    }

    public int downloadFromLink(ProjectRecord projectRecord) {
        return downloadFromLink(projectRecord.getLink(), PATH_TO_PLUGINS_FOLDER + projectRecord.getName() + ".jar");
    }

    public int downloadFromLink(String link, String path) {
        try {
            return readAndWrite(link, path);
        } catch (IOException e) {
            logger.log("Something went wrong downloading from a link.");
            return -1;
        }
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