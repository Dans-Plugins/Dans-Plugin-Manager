package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.utils.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DownloadServiceTest {

    // Only readAndWrite() is exercised here; the other dependencies are unused.
    private final DownloadService service = new DownloadService(null, null, null, null);

    // -------------------------------------------------------------------------
    // readAndWrite()
    // -------------------------------------------------------------------------

    @Test
    void readAndWrite_writesSourceBytesToDest(@TempDir Path tempDir) throws IOException {
        byte[] content = {1, 2, 3, 4, 5};
        File src = tempDir.resolve("source.bin").toFile();
        Files.write(src.toPath(), content);

        File dest = tempDir.resolve("dest.jar").toFile();
        int bytes = service.readAndWrite(src.toURI().toString(), dest.getAbsolutePath());

        assertEquals(content.length, bytes);
        assertArrayEquals(content, Files.readAllBytes(dest.toPath()));
    }

    @Test
    void readAndWrite_returnsZeroForEmptySource(@TempDir Path tempDir) throws IOException {
        File src = tempDir.resolve("empty.bin").toFile();
        src.createNewFile();

        File dest = tempDir.resolve("dest.jar").toFile();
        int bytes = service.readAndWrite(src.toURI().toString(), dest.getAbsolutePath());

        assertEquals(0, bytes);
        assertTrue(dest.exists());
    }

    @Test
    void readAndWrite_deletesPartialFileOnFailure(@TempDir Path tempDir) throws IOException {
        // Pre-create a file at the destination to simulate a partial download artifact.
        File partial = tempDir.resolve("partial.jar").toFile();
        partial.createNewFile();

        // Port 1 is privileged and always refused — guarantees a connection error.
        assertThrows(IOException.class, () ->
                service.readAndWrite("http://localhost:1/nonexistent.jar", partial.getAbsolutePath())
        );

        assertFalse(partial.exists(), "Partial file must be deleted after a failed download");
    }

    @Test
    void readAndWrite_throwsOnBadUrl(@TempDir Path tempDir) {
        File dest = tempDir.resolve("dest.jar").toFile();
        assertThrows(IOException.class, () ->
                service.readAndWrite("not-a-valid-url", dest.getAbsolutePath())
        );
    }

    // -------------------------------------------------------------------------
    // downloadLatest()
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_persistsTagAfterSuccessfulDownload(@TempDir Path tempDir) throws IOException {
        byte[] content = {1, 2, 3};
        File src = tempDir.resolve("plugin.jar").toFile();
        Files.write(src.toPath(), content);

        VersionStore versionStore = versionStore(tempDir);
        PluginFolderService pluginFolderService = new PluginFolderService(tempDir.toString());
        DownloadService svc = new DownloadService(null, fakeRelease("v1.0.0", src.toURI().toString()),
                pluginFolderService, versionStore);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        int result = svc.downloadLatest(record);

        assertTrue(result > 0);
        assertEquals("v1.0.0", versionStore.getStoredTag("testplugin"));
    }

    @Test
    void downloadLatest_returnsAlreadyUpToDate_whenTagMatchesAndJarPresent(@TempDir Path tempDir) throws IOException {
        VersionStore versionStore = versionStore(tempDir);
        versionStore.setTag("testplugin", "v1.0.0");
        Files.write(tempDir.resolve("testplugin.jar"), new byte[]{1});

        PluginFolderService pluginFolderService = new PluginFolderService(tempDir.toString());
        DownloadService svc = new DownloadService(null, fakeRelease("v1.0.0", "http://unused"),
                pluginFolderService, versionStore);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.ALREADY_UP_TO_DATE, svc.downloadLatest(record));
    }

    @Test
    void downloadLatest_reDownloads_whenTagMatchesButJarMissing(@TempDir Path tempDir) throws IOException {
        // JAR was manually deleted — must re-download even though the stored tag matches.
        VersionStore versionStore = versionStore(tempDir);
        versionStore.setTag("testplugin", "v1.0.0");

        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        PluginFolderService pluginFolderService = new PluginFolderService(tempDir.toString());
        DownloadService svc = new DownloadService(null, fakeRelease("v1.0.0", src.toURI().toString()),
                pluginFolderService, versionStore);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record) > 0, "Should re-download when JAR is absent despite matching tag");
    }

    @Test
    void downloadLatest_returnsNoRelease_whenGitHubReportsNone(@TempDir Path tempDir) {
        PluginFolderService pluginFolderService = new PluginFolderService(tempDir.toString());
        GitHubReleaseService noReleaseService = new GitHubReleaseService(null) {
            @Override
            public ReleaseInfo getLatestRelease(String owner, String repo) {
                return ReleaseInfo.NO_RELEASE;
            }
        };
        DownloadService svc = new DownloadService(null, noReleaseService, pluginFolderService, versionStore(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.NO_RELEASE, svc.downloadLatest(record));
    }

    // -------------------------------------------------------------------------
    // downloadLatest(record, physicallyInstalled)
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_withTrueHint_returnsAlreadyUpToDate_withoutDirectoryScan(@TempDir Path tempDir) {
        VersionStore versionStore = versionStore(tempDir);
        versionStore.setTag("testplugin", "v1.0.0");

        // PluginFolderService with a non-existent folder — isInstalled() would return false
        // if called, but the hint bypasses the scan entirely.
        PluginFolderService noScanService = new PluginFolderService("/this/path/does/not/exist");
        DownloadService svc = new DownloadService(null, fakeRelease("v1.0.0", "http://unused"),
                noScanService, versionStore);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.ALREADY_UP_TO_DATE, svc.downloadLatest(record, true));
    }

    @Test
    void downloadLatest_withFalseHint_downloadsEvenWhenTagMatches(@TempDir Path tempDir) throws IOException {
        VersionStore versionStore = versionStore(tempDir);
        versionStore.setTag("testplugin", "v1.0.0");

        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        PluginFolderService pluginFolderService = new PluginFolderService(tempDir.toString());
        DownloadService svc = new DownloadService(null, fakeRelease("v1.0.0", src.toURI().toString()),
                pluginFolderService, versionStore);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record, false) > 0,
                "physicallyInstalled=false must trigger download even when tag matches");
    }

    @Test
    void downloadLatest_noArgOverload_callsIsInstalledViaScan(@TempDir Path tempDir) throws IOException {
        VersionStore versionStore = versionStore(tempDir);
        versionStore.setTag("testplugin", "v1.0.0");
        Files.write(tempDir.resolve("testplugin.jar"), new byte[]{1});

        PluginFolderService pluginFolderService = new PluginFolderService(tempDir.toString());
        DownloadService svc = new DownloadService(null, fakeRelease("v1.0.0", "http://unused"),
                pluginFolderService, versionStore);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.ALREADY_UP_TO_DATE, svc.downloadLatest(record));
    }

    // -------------------------------------------------------------------------
    // downloadFromUrl — error code distinction
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_returnsNetworkError_whenUrlUnreachable(@TempDir Path tempDir) {
        Logger noOpLogger = new Logger(null) {
            @Override public void log(String message) {}
        };
        PluginFolderService pluginFolderService = new PluginFolderService(tempDir.toString());
        DownloadService svc = new DownloadService(noOpLogger,
                fakeRelease("v1.0.0", "http://localhost:1/nonexistent.jar"),
                pluginFolderService, versionStore(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.NETWORK_ERROR, svc.downloadLatest(record));
    }

    @Test
    void downloadLatest_returnsFileError_whenDestinationUnwritable(@TempDir Path tempDir) throws IOException {
        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        File readOnlyDir = tempDir.resolve("readonly").toFile();
        readOnlyDir.mkdir();
        // setWritable(false) is a no-op when running as root (common in some CI environments).
        assumeTrue(readOnlyDir.setWritable(false), "Skipped: cannot make directory read-only");

        Logger noOpLogger = new Logger(null) {
            @Override public void log(String message) {}
        };
        PluginFolderService pluginFolderService = new PluginFolderService(readOnlyDir.getAbsolutePath() + "/");
        DownloadService svc = new DownloadService(noOpLogger,
                fakeRelease("v1.0.0", src.toURI().toString()),
                pluginFolderService, versionStore(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.FILE_ERROR, svc.downloadLatest(record));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private VersionStore versionStore(Path tempDir) {
        return new VersionStore(tempDir.resolve("dpm-versions.properties").toFile());
    }

    private GitHubReleaseService fakeRelease(String tag, String jarUrl) {
        return new GitHubReleaseService(null) {
            @Override
            public ReleaseInfo getLatestRelease(String owner, String repo) {
                return new ReleaseInfo(tag, jarUrl);
            }
        };
    }
}
