package dansplugins.dpm.install.ports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Contract every JarDownloader must meet. */
public abstract class JarDownloaderContract {

    /** A downloader that serves {@code bytes} at the returned URL and nothing at any other URL. */
    protected abstract JarDownloader serving(byte[] bytes, Path workDir) throws IOException;

    /** The URL {@link #serving} serves at. */
    protected abstract String servedUrl(Path workDir);

    // -------------------------------------------------------------------------
    // download()
    // -------------------------------------------------------------------------

    @Test
    void download_writesTheBytesAndReturnsTheirCount(@TempDir Path tempDir) throws IOException {
        byte[] content = {1, 2, 3, 4};
        JarDownloader downloader = serving(content, tempDir);
        File dest = tempDir.resolve("Plugin.jar").toFile();
        assertEquals(4, downloader.download(servedUrl(tempDir), dest.getPath()));
        assertArrayEquals(content, Files.readAllBytes(dest.toPath()));
    }

    @Test
    void download_returnsNetworkErrorForAnUnreachableUrl(@TempDir Path tempDir) throws IOException {
        JarDownloader downloader = serving(new byte[]{1}, tempDir);
        File dest = tempDir.resolve("Plugin.jar").toFile();
        assertEquals(JarDownloader.NETWORK_ERROR,
                downloader.download(tempDir.resolve("absent.bin").toUri().toString(), dest.getPath()));
        assertFalse(dest.exists());
    }

    @Test
    void download_returnsFileErrorAndLeavesNoFileWhenTheDestinationCannotBeWritten(@TempDir Path tempDir) throws IOException {
        JarDownloader downloader = serving(new byte[]{1, 2}, tempDir);
        // A missing parent directory refuses the write even for root, unlike setWritable(false).
        File dest = tempDir.resolve("no-such-dir").resolve("Plugin.jar").toFile();
        assertEquals(JarDownloader.FILE_ERROR, downloader.download(servedUrl(tempDir), dest.getPath()));
        assertFalse(dest.exists());
    }
}
