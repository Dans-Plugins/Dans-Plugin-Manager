package dansplugins.dpm.install.ports;

import dansplugins.dpm.objects.ProjectRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/** Contract every PluginFolder must meet. Jars here carry no plugin.yml, so conflicts are by name. */
public abstract class PluginFolderContract {

    /** A folder rooted at {@code dir} (with a trailing separator) holding empty jars with these names. */
    protected abstract PluginFolder folderWith(Path dir, String... jarNames) throws Exception;

    private static ProjectRecord record(String name) {
        return ProjectRecord.builder(name, "Dans-Plugins", name).build();
    }

    // -------------------------------------------------------------------------
    // isInstalled() / filterInstalled()
    // -------------------------------------------------------------------------

    @Test
    void isInstalled_matchesTheManagedJarIgnoringCase(@TempDir Path tempDir) throws Exception {
        PluginFolder folder = folderWith(tempDir, "herald.jar");
        assertTrue(folder.isInstalled(record("Herald")));
        assertFalse(folder.isInstalled(record("Fiefs")));
    }

    @Test
    void isInstalled_ignoresAVersionedJar(@TempDir Path tempDir) throws Exception {
        assertFalse(folderWith(tempDir, "Herald-1.2.0.jar").isInstalled(record("Herald")));
    }

    @Test
    void filterInstalled_keepsInputOrder(@TempDir Path tempDir) throws Exception {
        PluginFolder folder = folderWith(tempDir, "B.jar", "A.jar");
        assertEquals(Arrays.asList("A", "B"),
                folder.filterInstalled(Arrays.asList(record("A"), record("C"), record("B")))
                        .stream().map(ProjectRecord::getName).collect(java.util.stream.Collectors.toList()));
    }

    // -------------------------------------------------------------------------
    // managedJarPath()
    // -------------------------------------------------------------------------

    @Test
    void managedJarPath_isNameDotJarInsideTheFolder(@TempDir Path tempDir) throws Exception {
        String path = folderWith(tempDir).managedJarPath(record("Herald"));
        assertEquals(tempDir.toString() + java.io.File.separator + "Herald.jar", path);
    }

    // -------------------------------------------------------------------------
    // conflictingJars() / deleteJar()
    // -------------------------------------------------------------------------

    @Test
    void conflictingJars_findsTheSamePluginUnderAnotherNameButNotTheManagedJar(@TempDir Path tempDir) throws Exception {
        PluginFolder folder = folderWith(tempDir, "Herald.jar", "Herald-1.2.0.jar", "Fiefs.jar");
        assertEquals(Collections.singletonList("Herald-1.2.0.jar"), folder.conflictingJars(record("Herald")));
    }

    @Test
    void deleteJar_removesTheJarAndReportsSuccess(@TempDir Path tempDir) throws Exception {
        PluginFolder folder = folderWith(tempDir, "Herald-1.2.0.jar");
        assertTrue(folder.deleteJar("Herald-1.2.0.jar"));
        assertTrue(folder.conflictingJars(record("Herald")).isEmpty());
    }

    @Test
    void deleteJar_returnsFalseForAMissingJar(@TempDir Path tempDir) throws Exception {
        assertFalse(folderWith(tempDir).deleteJar("Nothing.jar"));
    }
}
