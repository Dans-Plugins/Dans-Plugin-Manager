package dansplugins.dpm.install.adapters;

import dansplugins.dpm.install.ports.JarDownloader;
import dansplugins.dpm.install.ports.JarDownloaderContract;
import dansplugins.dpm.utils.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Served over file:// so the contract runs without a network; the retry sleep is skipped.
class HttpJarDownloaderContractTest extends JarDownloaderContract {
    @Override
    protected JarDownloader serving(byte[] bytes, Path workDir) throws IOException {
        Files.write(workDir.resolve("served.bin"), bytes);
        return new NoSleepHttpJarDownloader(new Logger(null) {
            @Override public void log(String m) {}
            @Override public void warn(String m) {}
        });
    }

    @Override
    protected String servedUrl(Path workDir) {
        return workDir.resolve("served.bin").toUri().toString();
    }
}
