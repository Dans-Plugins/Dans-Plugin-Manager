package dansplugins.dpm.install.adapters;

import dansplugins.dpm.install.ports.DebugLog;
import dansplugins.dpm.install.ports.JarDownloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/** Downloads a jar over HTTP(S) (or any URL scheme Java opens), retrying once after a failed open. */
public class HttpJarDownloader implements JarDownloader {
    private final DebugLog logger;

    public HttpJarDownloader(DebugLog logger) {
        this.logger = logger;
    }

    @Override
    public int download(String url, String path) {
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
