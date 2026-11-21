package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.JarDownloader;
import dansplugins.dpm.install.ports.JarDownloaderContract;

import java.nio.file.Path;

class FakeJarDownloaderContractTest extends JarDownloaderContract {
    @Override
    protected JarDownloader serving(byte[] bytes, Path workDir) {
        return new FakeJarDownloader().serve(servedUrl(workDir), bytes);
    }

    @Override
    protected String servedUrl(Path workDir) {
        return "http://example.invalid/Plugin.jar";
    }
}
