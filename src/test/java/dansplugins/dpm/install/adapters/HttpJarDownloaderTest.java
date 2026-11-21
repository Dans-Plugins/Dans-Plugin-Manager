package dansplugins.dpm.install.adapters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Moved from DownloadServiceTest when the network code moved into HttpJarDownloader; assertions unchanged.
class HttpJarDownloaderTest {

    // sleepMs is overridden to a no-op so retry tests don't add real delays.
    private final HttpJarDownloader downloader = new NoSleepHttpJarDownloader(null);

    // -------------------------------------------------------------------------
    // readAndWrite()
    // -------------------------------------------------------------------------

    @Test
    void readAndWrite_writesSourceBytesToDest(@TempDir Path tempDir) throws IOException {
        byte[] content = {1, 2, 3, 4, 5};
        File src = tempDir.resolve("source.bin").toFile();
        Files.write(src.toPath(), content);

        File dest = tempDir.resolve("dest.jar").toFile();
        int bytes = downloader.readAndWrite(src.toURI().toString(), dest.getAbsolutePath());

        assertEquals(content.length, bytes);
        assertArrayEquals(content, Files.readAllBytes(dest.toPath()));
    }

    @Test
    void readAndWrite_returnsZeroForEmptySource(@TempDir Path tempDir) throws IOException {
        File src = tempDir.resolve("empty.bin").toFile();
        src.createNewFile();

        File dest = tempDir.resolve("dest.jar").toFile();
        int bytes = downloader.readAndWrite(src.toURI().toString(), dest.getAbsolutePath());

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
                downloader.readAndWrite("http://localhost:1/nonexistent.jar", partial.getAbsolutePath())
        );

        assertFalse(partial.exists(), "Partial file must be deleted after a failed download");
    }

    @Test
    void readAndWrite_throwsOnBadUrl(@TempDir Path tempDir) {
        File dest = tempDir.resolve("dest.jar").toFile();
        assertThrows(IOException.class, () ->
                downloader.readAndWrite("not-a-valid-url", dest.getAbsolutePath())
        );
    }

    // -------------------------------------------------------------------------
    // openNetworkStream() — retry on transient failure
    // -------------------------------------------------------------------------

    @Test
    void openNetworkStream_retriesOnce_afterIoException(@TempDir Path tempDir) throws IOException {
        byte[] content = {1, 2, 3};
        File src = tempDir.resolve("source.jar").toFile();
        Files.write(src.toPath(), content);

        int[] callCount = {0};
        HttpJarDownloader svc = new HttpJarDownloader(null) {
            @Override void sleepMs(long ms) {}
            @Override BufferedInputStream doOpenNetworkStream(String url) throws IOException {
                if (callCount[0]++ == 0) throw new IOException("transient failure");
                return super.doOpenNetworkStream(url);
            }
        };
        File dest = tempDir.resolve("dest.jar").toFile();
        int bytes = svc.readAndWrite(src.toURI().toString(), dest.getAbsolutePath());
        assertEquals(content.length, bytes, "Second attempt must succeed after transient failure");
        assertEquals(2, callCount[0], "doOpenNetworkStream must be called exactly twice");
    }

    @Test
    void openNetworkStream_throwsAfterBothAttemptsFail(@TempDir Path tempDir) {
        int[] callCount = {0};
        HttpJarDownloader svc = new HttpJarDownloader(null) {
            @Override void sleepMs(long ms) {}
            @Override BufferedInputStream doOpenNetworkStream(String url) throws IOException {
                callCount[0]++;
                throw new IOException("persistent failure");
            }
        };
        File dest = tempDir.resolve("dest.jar").toFile();
        assertThrows(IOException.class, () -> svc.readAndWrite("http://example.invalid/test.jar", dest.getAbsolutePath()));
        assertEquals(2, callCount[0], "doOpenNetworkStream must be called exactly twice before giving up");
    }
}
