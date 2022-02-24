package dansplugins.dpm.services;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.utils.Logger;

public class LocalDownloadService {
    private static LocalDownloadService instance;
    private final static String PATH_TO_PLUGINS_FOLDER = "./plugins/";

    private LocalDownloadService() {

    }

    public static LocalDownloadService getInstance() {
        if (instance == null) {
            instance = new LocalDownloadService();
        }
        return instance;
    }

    public int downloadFromLink(ProjectRecord projectRecord) {
        return downloadFromLink(projectRecord.getLink(), PATH_TO_PLUGINS_FOLDER + projectRecord.getName() + ".jar");
    }

    public int downloadFromLink(String link, String path) {
        try {
            int bytesRead = readAndWrite(link, path);
            return bytesRead;
        } catch (IOException e) {
            Logger.getInstance().log("Something went wrong downloading from a link.");
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

    public static void main(String[] args) {
        LocalDownloadService localDownloadService = new LocalDownloadService();
        ProjectRecord record = new ProjectRecord("medievalfactions", "https://github.com/Dans-Plugins/Medieval-Factions/releases/download/v4.6.2/Medieval-Factions-4.6.2.jar");
        int bytesRead = localDownloadService.downloadFromLink(record.getLink(), "./" + record.getName() + ".jar");
        assert(bytesRead != -1);
    }
}