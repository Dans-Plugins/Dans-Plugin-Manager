package dansplugins.dpm.install.core;

import dansplugins.dpm.install.fakes.FakeDebugLog;
import dansplugins.dpm.install.fakes.FakeJarDownloader;
import dansplugins.dpm.install.fakes.FakePluginFolder;
import dansplugins.dpm.install.fakes.FakeReleaseSource;
import dansplugins.dpm.install.fakes.FakeVersionStore;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

// DownloadService against in-memory ports: no network, and failure paths that need no file
// permissions, so they run the same as root (where setWritable-based tests are skipped).
class DownloadServicePortsTest {

    private static final String URL = "http://example.invalid/Herald.jar";
    private final ProjectRecord herald = ProjectRecord.builder("Herald", "Dans-Plugins", "Herald").build();
    private final FakeReleaseSource releases = new FakeReleaseSource()
            .publish("Dans-Plugins", "Herald", ReleaseChannel.STABLE, new ReleaseInfo("v2.0.0", URL));
    private final FakeVersionStore versions = new FakeVersionStore();
    private final FakeDebugLog log = new FakeDebugLog();

    private DownloadService service(FakePluginFolder folder, FakeJarDownloader downloader) {
        return new DownloadService(log, releases, folder, versions, downloader);
    }

    // -------------------------------------------------------------------------
    // downloadLatest()
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_returnsFileError_andKeepsConflictsAndTag_whenTheWriteFails(@TempDir Path tempDir) {
        FakePluginFolder folder = new FakePluginFolder(tempDir + File.separator).with("Herald-1.0.0.jar");
        versions.setTag("Herald", "v1.0.0");

        int result = service(folder, new FakeJarDownloader().serve(URL, new byte[]{1}).failWrites())
                .downloadLatest(herald);

        assertEquals(DownloadService.FILE_ERROR, result);
        assertTrue(folder.jars.contains("Herald-1.0.0.jar"), "a failed install must not delete the old jar");
        assertEquals("v1.0.0", versions.getStoredTag("Herald"));
    }

    @Test
    void downloadLatest_removesConflictsAndStoresTheTag_afterASuccessfulDownload(@TempDir Path tempDir) {
        FakePluginFolder folder = new FakePluginFolder(tempDir + File.separator).with("Herald-1.0.0.jar");

        int result = service(folder, new FakeJarDownloader().serve(URL, new byte[]{1, 2, 3})).downloadLatest(herald);

        assertEquals(3, result);
        assertFalse(folder.jars.contains("Herald-1.0.0.jar"));
        assertEquals("v2.0.0", versions.getStoredTag("Herald"));
        assertEquals(Collections.singletonList("Removed older JAR after download: Herald-1.0.0.jar"), log.debug);
    }

    @Test
    void downloadLatest_logsButSucceeds_whenAConflictCannotBeDeleted(@TempDir Path tempDir) {
        FakePluginFolder folder = new FakePluginFolder(tempDir + File.separator).with("Herald-1.0.0.jar");
        folder.undeletable.add("Herald-1.0.0.jar");

        int result = service(folder, new FakeJarDownloader().serve(URL, new byte[]{1})).downloadLatest(herald);

        assertEquals(1, result);
        assertEquals(Collections.singletonList("Failed to remove older JAR: Herald-1.0.0.jar"), log.debug);
    }

    @Test
    void downloadLatest_returnsNetworkErrorAndWarns_whenTheReleaseSourceIsUnreachable(@TempDir Path tempDir) {
        releases.unreachable("Dans-Plugins", "Herald", ReleaseChannel.STABLE);
        FakePluginFolder folder = new FakePluginFolder(tempDir + File.separator);

        int result = service(folder, new FakeJarDownloader()).downloadLatest(herald);

        assertEquals(DownloadService.NETWORK_ERROR, result);
        assertEquals(Collections.singletonList("Could not resolve release info for Herald."), log.warnings);
    }

    @Test
    void downloadLatest_withTrueHint_neverScansTheFolder(@TempDir Path tempDir) {
        FakePluginFolder folder = new FakePluginFolder(tempDir + File.separator);
        versions.setTag("Herald", "v2.0.0");

        assertEquals(DownloadService.ALREADY_UP_TO_DATE,
                service(folder, new FakeJarDownloader()).downloadLatest(herald, true));
        assertEquals(0, folder.scans);
    }
}
