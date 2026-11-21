package dansplugins.dpm.install.core;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.install.adapters.GitHubReleaseRepository;
import dansplugins.dpm.install.adapters.NoSleepHttpJarDownloader;
import dansplugins.dpm.install.adapters.PluginFileRepository;
import dansplugins.dpm.install.adapters.VersionRepository;
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

    // sleepMs is overridden to a no-op (via NoSleepHttpJarDownloader) so retries don't add real delays.
    private static DownloadService noSleepService(Logger logger, GitHubReleaseRepository ghrs,
                                                   PluginFileRepository pfs, VersionRepository vr) {
        return new DownloadService(logger, ghrs, pfs, vr, new NoSleepHttpJarDownloader(logger));
    }

    // Reports whether the directory is now genuinely unwritable, not merely whether the chmod
    // succeeded: as root, setWritable(false) returns true yet the mode bits are bypassed, so a guard
    // on its return value alone would run the test and watch the "unwritable" write succeed (#120).
    private static boolean makeReadOnly(File dir) {
        if (!dir.setWritable(false)) return false;
        File probe = new File(dir, ".write-probe");
        try {
            if (!probe.createNewFile()) return false;
        } catch (IOException e) {
            return true; // the write was refused, so the restriction holds for this user
        }
        probe.delete();
        return false;
    }

    // -------------------------------------------------------------------------
    // downloadLatest()
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_persistsTagAfterSuccessfulDownload(@TempDir Path tempDir) throws IOException {
        byte[] content = {1, 2, 3};
        File src = tempDir.resolve("plugin.jar").toFile();
        Files.write(src.toPath(), content);

        VersionRepository versionRepository = versionRepository(tempDir);
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null, fakeRelease("v1.0.0", src.toURI().toString()),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        int result = svc.downloadLatest(record);

        assertTrue(result > 0);
        assertEquals("v1.0.0", versionRepository.getStoredTag("testplugin"));
    }

    @Test
    void downloadLatest_returnsAlreadyUpToDate_whenTagMatchesAndJarPresent(@TempDir Path tempDir) throws IOException {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("testplugin", "v1.0.0");
        Files.write(tempDir.resolve("testplugin.jar"), new byte[]{1});

        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null, fakeRelease("v1.0.0", "http://unused"),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.ALREADY_UP_TO_DATE, svc.downloadLatest(record));
    }

    @Test
    void downloadLatest_reDownloads_whenTagMatchesButJarMissing(@TempDir Path tempDir) throws IOException {
        // JAR was manually deleted — must re-download even though the stored tag matches.
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("testplugin", "v1.0.0");

        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null, fakeRelease("v1.0.0", src.toURI().toString()),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record) > 0, "Should re-download when JAR is absent despite matching tag");
    }

    @Test
    void downloadLatest_returnsNoRelease_whenGitHubReportsNone(@TempDir Path tempDir) {
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        GitHubReleaseRepository noReleaseService = new GitHubReleaseRepository(null) {
            @Override
            public ReleaseInfo getLatestRelease(String owner, String repo) {
                return ReleaseInfo.NO_RELEASE;
            }
        };
        DownloadService svc = noSleepService(null, noReleaseService, pluginFileRepository, versionRepository(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.NO_RELEASE, svc.downloadLatest(record));
    }

    // -------------------------------------------------------------------------
    // downloadLatest(record, physicallyInstalled)
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_withTrueHint_returnsAlreadyUpToDate_withoutDirectoryScan(@TempDir Path tempDir) {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("testplugin", "v1.0.0");

        // PluginFileRepository with a non-existent folder — isInstalled() would return false
        // if called, but the hint bypasses the scan entirely.
        PluginFileRepository noScanService = new PluginFileRepository("/this/path/does/not/exist");
        DownloadService svc = noSleepService(null, fakeRelease("v1.0.0", "http://unused"),
                noScanService, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.ALREADY_UP_TO_DATE, svc.downloadLatest(record, true));
    }

    @Test
    void downloadLatest_withFalseHint_downloadsEvenWhenTagMatches(@TempDir Path tempDir) throws IOException {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("testplugin", "v1.0.0");

        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null, fakeRelease("v1.0.0", src.toURI().toString()),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record, false) > 0,
                "physicallyInstalled=false must trigger download even when tag matches");
    }

    @Test
    void downloadLatest_noArgOverload_callsIsInstalledViaScan(@TempDir Path tempDir) throws IOException {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("testplugin", "v1.0.0");
        Files.write(tempDir.resolve("testplugin.jar"), new byte[]{1});

        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null, fakeRelease("v1.0.0", "http://unused"),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.ALREADY_UP_TO_DATE, svc.downloadLatest(record));
    }

    // -------------------------------------------------------------------------
    // downloadLatest(record, channel[, physicallyInstalled])
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_experimentalChannel_persistsTheSynthesizedDevVersion(@TempDir Path tempDir) throws IOException {
        File src = tempDir.resolve("plugin.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        VersionRepository versionRepository = versionRepository(tempDir);
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null,
                fakeChannelledRelease("v1.0.0", "dev-abc1234", src.toURI().toString()),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record, ReleaseChannel.EXPERIMENTAL) > 0);
        assertEquals("dev-abc1234", versionRepository.getStoredTag("testplugin"));
    }

    @Test
    void downloadLatest_switchingFromStableToExperimental_reDownloadsEvenThoughTheJarIsPresent(@TempDir Path tempDir) throws IOException {
        // Installed on stable at v1.0.0, with the JAR physically present.
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("testplugin", "v1.0.0");
        Files.write(tempDir.resolve("testplugin.jar"), new byte[]{9});

        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null,
                fakeChannelledRelease("v1.0.0", "dev-abc1234", src.toURI().toString()),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record, ReleaseChannel.EXPERIMENTAL) > 0,
                "A channel switch must re-download — the other channel's tag never matches the stored one");
        assertEquals("dev-abc1234", versionRepository.getStoredTag("testplugin"));
    }

    @Test
    void downloadLatest_switchingFromExperimentalBackToStable_reDownloads(@TempDir Path tempDir) throws IOException {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("testplugin", "dev-abc1234");
        Files.write(tempDir.resolve("testplugin.jar"), new byte[]{9});

        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null,
                fakeChannelledRelease("v1.0.0", "dev-abc1234", src.toURI().toString()),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record, ReleaseChannel.STABLE) > 0);
        assertEquals("v1.0.0", versionRepository.getStoredTag("testplugin"));
    }

    @Test
    void downloadLatest_experimentalChannel_returnsAlreadyUpToDateWhenTheDevBuildIsUnchanged(@TempDir Path tempDir) throws IOException {
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("testplugin", "dev-abc1234");
        Files.write(tempDir.resolve("testplugin.jar"), new byte[]{1});

        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null,
                fakeChannelledRelease("v1.0.0", "dev-abc1234", "http://unused"),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.ALREADY_UP_TO_DATE, svc.downloadLatest(record, ReleaseChannel.EXPERIMENTAL));
    }

    @Test
    void downloadLatest_experimentalChannel_returnsNoReleaseWhenRepoPublishesNoDevBuild(@TempDir Path tempDir) {
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        GitHubReleaseRepository noDevBuild = new GitHubReleaseRepository(null) {
            @Override
            public ReleaseInfo getExperimentalRelease(String owner, String repo) {
                return ReleaseInfo.NO_RELEASE;
            }
        };
        DownloadService svc = noSleepService(null, noDevBuild, pluginFileRepository, versionRepository(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.NO_RELEASE, svc.downloadLatest(record, ReleaseChannel.EXPERIMENTAL));
    }

    @Test
    void downloadLatest_defaultOverloadsStillUseTheStableChannel(@TempDir Path tempDir) throws IOException {
        File src = tempDir.resolve("plugin.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        VersionRepository versionRepository = versionRepository(tempDir);
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(null,
                fakeChannelledRelease("v1.0.0", "dev-abc1234", src.toURI().toString()),
                pluginFileRepository, versionRepository);

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record) > 0);
        assertEquals("v1.0.0", versionRepository.getStoredTag("testplugin"));
    }

    // -------------------------------------------------------------------------
    // removeConflictingJars — ordering relative to download
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_doesNotRemoveConflictingJars_whenDownloadFails(@TempDir Path tempDir) throws IOException {
        File conflicting = tempDir.resolve("testplugin-1.0.0.jar").toFile();
        Files.write(conflicting.toPath(), new byte[]{1, 2, 3});

        Logger noOpLogger = new Logger(null) {
            @Override public void log(String message) {}
            @Override public void warn(String message) {}
        };
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(noOpLogger,
                fakeRelease("v2.0.0", "http://localhost:1/nonexistent.jar"),
                pluginFileRepository, versionRepository(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.NETWORK_ERROR, svc.downloadLatest(record));
        assertTrue(conflicting.exists(), "Conflicting JAR must survive a failed download");
    }

    @Test
    void downloadLatest_removesConflictingJars_afterSuccessfulDownload(@TempDir Path tempDir) throws IOException {
        File conflicting = tempDir.resolve("testplugin-1.0.0.jar").toFile();
        Files.write(conflicting.toPath(), new byte[]{1, 2, 3});

        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{4, 5, 6});

        Logger noOpLogger = new Logger(null) {
            @Override public void log(String message) {}
            @Override public void warn(String message) {}
        };
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(noOpLogger,
                fakeRelease("v2.0.0", src.toURI().toString()),
                pluginFileRepository, versionRepository(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertTrue(svc.downloadLatest(record) > 0);
        assertFalse(conflicting.exists(), "Conflicting JAR must be removed after a successful download");
    }

    // -------------------------------------------------------------------------
    // downloadFromUrl — error code distinction
    // -------------------------------------------------------------------------

    @Test
    void downloadLatest_returnsNetworkError_whenUrlUnreachable(@TempDir Path tempDir) {
        Logger noOpLogger = new Logger(null) {
            @Override public void log(String message) {}
            @Override public void warn(String message) {}
        };
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        DownloadService svc = noSleepService(noOpLogger,
                fakeRelease("v1.0.0", "http://localhost:1/nonexistent.jar"),
                pluginFileRepository, versionRepository(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.NETWORK_ERROR, svc.downloadLatest(record));
    }

    @Test
    void downloadLatest_returnsFileError_whenDestinationUnwritable(@TempDir Path tempDir) throws IOException {
        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), new byte[]{1, 2, 3});

        File readOnlyDir = tempDir.resolve("readonly").toFile();
        readOnlyDir.mkdir();
        assumeTrue(makeReadOnly(readOnlyDir), "Skipped: directory permissions are not enforced for this user");

        Logger noOpLogger = new Logger(null) {
            @Override public void log(String message) {}
            @Override public void warn(String message) {}
        };
        PluginFileRepository pluginFileRepository = new PluginFileRepository(readOnlyDir.getAbsolutePath() + "/");
        DownloadService svc = noSleepService(noOpLogger,
                fakeRelease("v1.0.0", src.toURI().toString()),
                pluginFileRepository, versionRepository(tempDir));

        ProjectRecord record = ProjectRecord.forGitHub("testplugin", "Org", "Repo");
        assertEquals(DownloadService.FILE_ERROR, svc.downloadLatest(record));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private VersionRepository versionRepository(Path tempDir) {
        Logger noOp = new Logger(null) {
            @Override public void log(String m) {}
            @Override public void warn(String m) {}
        };
        return new VersionRepository(tempDir.resolve("dpm-versions.properties").toFile(), noOp);
    }

    private GitHubReleaseRepository fakeRelease(String tag, String jarUrl) {
        return new GitHubReleaseRepository(null) {
            @Override
            public ReleaseInfo getLatestRelease(String owner, String repo) {
                return new ReleaseInfo(tag, jarUrl);
            }
        };
    }

    // Serves a different tag per channel, as the real repository does.
    private GitHubReleaseRepository fakeChannelledRelease(String stableTag, String experimentalTag, String jarUrl) {
        return new GitHubReleaseRepository(null) {
            @Override
            public ReleaseInfo getLatestRelease(String owner, String repo) {
                return new ReleaseInfo(stableTag, jarUrl);
            }
            @Override
            public ReleaseInfo getExperimentalRelease(String owner, String repo) {
                return new ReleaseInfo(experimentalTag, jarUrl);
            }
        };
    }
}
