package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginFolderServiceTest {

    private final PluginFolderService service = new PluginFolderService();

    // -------------------------------------------------------------------------
    // normalize()
    // -------------------------------------------------------------------------

    @Test
    void normalize_stripsExtension() {
        assertEquals("medievalfactions", service.normalize("medievalfactions.jar"));
    }

    @Test
    void normalize_stripsVersionSuffix() {
        assertEquals("medievalfactions", service.normalize("Medieval-Factions-4.6.3.jar"));
    }

    @Test
    void normalize_stripsVersionSuffixWithV() {
        assertEquals("activitytracker", service.normalize("ActivityTracker-v1.0.jar"));
    }

    @Test
    void normalize_stripsUnderscoreSeparators() {
        assertEquals("bluemapmedievalfactions", service.normalize("Bluemap_MedievalFactions.jar"));
    }

    @Test
    void normalize_stripsHyphensWithNoVersion() {
        assertEquals("dansessentials", service.normalize("Dans-Essentials.jar"));
    }

    @Test
    void normalize_stripsHyphensAndVersion() {
        assertEquals("dansessentials", service.normalize("Dans-Essentials-2.2.jar"));
    }

    @Test
    void normalize_camelCaseNoSeparators() {
        assertEquals("wildpets", service.normalize("WildPets-1.4.jar"));
    }

    @Test
    void normalize_singleWordPlugin() {
        assertEquals("mailboxes", service.normalize("Mailboxes-v1.1.jar"));
    }

    @Test
    void normalize_alreadyNormalized() {
        assertEquals("simpleskills", service.normalize("simpleskills.jar"));
    }

    @Test
    void normalize_mixedCaseExtension() {
        assertEquals("currencies", service.normalize("Currencies.JAR"));
    }

    // -------------------------------------------------------------------------
    // findConflictingJars()
    // -------------------------------------------------------------------------

    @Test
    void findConflictingJars_returnsVersionedCopy(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");
        createFile(tempDir, "medievalfactions.jar");

        PluginFolderService svc = new PluginFolderService(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        List<File> conflicts = svc.findConflictingJars(record);
        assertEquals(1, conflicts.size());
        assertEquals("Medieval-Factions-4.6.3.jar", conflicts.get(0).getName());
    }

    @Test
    void findConflictingJars_ignoresManagedFile(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "medievalfactions.jar");

        PluginFolderService svc = new PluginFolderService(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_ignoresDifferentPlugin(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");

        PluginFolderService svc = new PluginFolderService(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_returnsMultipleConflicts(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.2.jar");
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");
        createFile(tempDir, "medievalfactions.jar");

        PluginFolderService svc = new PluginFolderService(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertEquals(2, svc.findConflictingJars(record).size());
    }

    @Test
    void findConflictingJars_emptyFolder(@TempDir Path tempDir) {
        PluginFolderService svc = new PluginFolderService(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_ignoresNonJarFiles(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.zip");
        createFile(tempDir, "Medieval-Factions-4.6.3.txt");

        PluginFolderService svc = new PluginFolderService(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    private void createFile(Path dir, String name) throws IOException {
        new File(dir.toFile(), name).createNewFile();
    }
}
