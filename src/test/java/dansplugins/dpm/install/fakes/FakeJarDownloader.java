package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.JarDownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** Serves bytes from memory; unknown URLs are unreachable. Writes really go to disk. */
public class FakeJarDownloader implements JarDownloader {
    private final Map<String, byte[]> served = new HashMap<>();
    private boolean failWrites;

    public FakeJarDownloader serve(String url, byte[] bytes) {
        served.put(url, bytes);
        return this;
    }

    /** Every write fails, as an unwritable plugins folder would (even for root). */
    public FakeJarDownloader failWrites() {
        failWrites = true;
        return this;
    }

    @Override
    public int download(String url, String destPath) {
        byte[] bytes = served.get(url);
        if (bytes == null) return NETWORK_ERROR;
        File dest = new File(destPath);
        try {
            if (failWrites) throw new IOException("write refused");
            Files.write(dest.toPath(), bytes);
            return bytes.length;
        } catch (IOException e) {
            dest.delete();
            return FILE_ERROR;
        }
    }
}
