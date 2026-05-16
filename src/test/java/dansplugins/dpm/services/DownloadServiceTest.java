package dansplugins.dpm.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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
}
