package dansplugins.dpm.repositories;

import dansplugins.dpm.objects.ProjectRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PluginFileRepositoryTest {

    private final PluginFileRepository service = new PluginFileRepository();

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

    @Test
    void normalize_stripsSnapshotQualifier() {
        assertEquals("plugin", service.normalize("Plugin-1.0-SNAPSHOT.jar"));
    }

    @Test
    void normalize_stripsAlphaQualifier() {
        // Real-world filename from MiniFactions release history
        assertEquals("minifactions", service.normalize("MiniFactions-0.1-ALPHA-4-17-2022.jar"));
    }

    @Test
    void normalize_stripsUnderscoreVersion() {
        assertEquals("wildpets", service.normalize("Wild_Pets_1.4.jar"));
    }

    @Test
    void normalize_preservesEmbeddedDigitsInName() {
        // The '3' is part of the plugin name, not a version — it must not be stripped
        assertEquals("dans3essentials", service.normalize("Dans3Essentials-2.0.jar"));
    }

    // -------------------------------------------------------------------------
    // findConflictingJars()
    // -------------------------------------------------------------------------

    @Test
    void findConflictingJars_returnsVersionedCopy(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");
        createFile(tempDir, "medievalfactions.jar");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        List<File> conflicts = svc.findConflictingJars(record);
        assertEquals(1, conflicts.size());
        assertEquals("Medieval-Factions-4.6.3.jar", conflicts.get(0).getName());
    }

    @Test
    void findConflictingJars_ignoresManagedFile(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "medievalfactions.jar");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_ignoresDifferentPlugin(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_returnsMultipleConflicts(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.2.jar");
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");
        createFile(tempDir, "medievalfactions.jar");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertEquals(2, svc.findConflictingJars(record).size());
    }

    @Test
    void findConflictingJars_emptyFolder(@TempDir Path tempDir) {
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_ignoresNonJarFiles(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.zip");
        createFile(tempDir, "Medieval-Factions-4.6.3.txt");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_reportsAJarWhoseNameDiffersOnlyInCase(@TempDir Path tempDir) throws IOException {
        // This previously asserted the opposite: that MEDIEVALFACTIONS.JAR was
        // "the canonical copy" and had to be excluded. It is not. Installs write
        // exactly "medievalfactions.jar", so on a case-sensitive filesystem the
        // two are separate files and excluding one leaves the server with two
        // jars for one plugin — which is what #130 was. Where case does not
        // distinguish files they are the same file, which the install replaces
        // anyway, so reporting it costs nothing.
        createFile(tempDir, "MEDIEVALFACTIONS.JAR");
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        List<File> conflicts = svc.findConflictingJars(record);
        List<String> names = conflicts.stream().map(File::getName).sorted().toList();
        assertEquals(List.of("MEDIEVALFACTIONS.JAR", "Medieval-Factions-4.6.3.jar"), names);
    }

    @Test
    void findConflictingJars_nonExistentFolderReturnsEmpty() {
        PluginFileRepository svc = new PluginFileRepository("/this/path/does/not/exist");
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    // -------------------------------------------------------------------------
    // isInstalled()
    // -------------------------------------------------------------------------

    @Test
    void isInstalled_returnsTrueWhenManagedJarExists(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "medievalfactions.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        assertTrue(svc.isInstalled(record));
    }

    @Test
    void isInstalled_returnsFalseWhenManagedJarAbsent(@TempDir Path tempDir) {
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        assertFalse(svc.isInstalled(record));
    }

    @Test
    void isInstalled_returnsFalseWhenOnlyVersionedJarPresent(@TempDir Path tempDir) throws IOException {
        // The versioned copy is a conflict, not the managed file
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        assertFalse(svc.isInstalled(record));
    }

    // -------------------------------------------------------------------------
    // filterInstalled()
    // -------------------------------------------------------------------------

    @Test
    void filterInstalled_returnsOnlyInstalledRecords(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "medievalfactions.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"),
                ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies")
        );
        List<ProjectRecord> result = svc.filterInstalled(records);
        assertEquals(1, result.size());
        assertEquals("medievalfactions", result.get(0).getName());
    }

    @Test
    void filterInstalled_returnsAllWhenAllInstalled(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "medievalfactions.jar");
        createFile(tempDir, "currencies.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"),
                ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies")
        );
        assertEquals(2, svc.filterInstalled(records).size());
    }

    @Test
    void filterInstalled_returnsEmptyWhenNoneInstalled(@TempDir Path tempDir) {
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions")
        );
        assertTrue(svc.filterInstalled(records).isEmpty());
    }

    @Test
    void filterInstalled_returnsEmptyForEmptyInput(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "medievalfactions.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        assertTrue(svc.filterInstalled(List.of()).isEmpty());
    }

    @Test
    void filterInstalled_isCaseInsensitive(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "MedievalFactions.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions")
        );
        assertEquals(1, svc.filterInstalled(records).size());
    }

    @Test
    void filterInstalled_nonExistentFolderReturnsEmpty() {
        PluginFileRepository svc = new PluginFileRepository("/this/path/does/not/exist");
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions")
        );
        assertTrue(svc.filterInstalled(records).isEmpty());
    }

    // -------------------------------------------------------------------------
    // getInstalledFile()
    // -------------------------------------------------------------------------

    @Test
    void getInstalledFile_returnsFileWhenPresent(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "medievalfactions.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString() + "/");
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        File result = svc.getInstalledFile(record);
        assertNotNull(result);
        assertEquals("medievalfactions.jar", result.getName());
    }

    @Test
    void getInstalledFile_returnsNullWhenAbsent(@TempDir Path tempDir) {
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString() + "/");
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        assertNull(svc.getInstalledFile(record));
    }

    @Test
    void getInstalledFile_caseInsensitiveMatch(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "MedievalFactions.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString() + "/");
        ProjectRecord record = ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions");
        assertNotNull(svc.getInstalledFile(record));
    }

    // -------------------------------------------------------------------------
    // findAllConflictingJars()
    // -------------------------------------------------------------------------

    @Test
    void findAllConflictingJars_returnsConflictsForAffectedPlugin(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");
        createFile(tempDir, "medievalfactions.jar");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"),
                ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies")
        );

        Map<String, List<File>> result = svc.findAllConflictingJars(records);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("medievalfactions"));
        assertEquals(1, result.get("medievalfactions").size());
        assertEquals("Medieval-Factions-4.6.3.jar", result.get("medievalfactions").get(0).getName());
    }

    @Test
    void findAllConflictingJars_emptyWhenNoConflicts(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "medievalfactions.jar");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions")
        );

        assertTrue(svc.findAllConflictingJars(records).isEmpty());
    }

    @Test
    void findAllConflictingJars_handlesMultiplePluginsWithConflicts(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");
        createFile(tempDir, "medievalfactions.jar");
        createFile(tempDir, "Currencies-2.1.jar");
        createFile(tempDir, "currencies.jar");

        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions"),
                ProjectRecord.forGitHub("currencies", "Dans-Plugins", "Currencies")
        );

        Map<String, List<File>> result = svc.findAllConflictingJars(records);
        assertEquals(2, result.size());
        assertEquals(1, result.get("medievalfactions").size());
        assertEquals(1, result.get("currencies").size());
    }

    @Test
    void findAllConflictingJars_emptyFolder(@TempDir Path tempDir) {
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions")
        );
        assertTrue(svc.findAllConflictingJars(records).isEmpty());
    }

    @Test
    void findAllConflictingJars_nonExistentFolderReturnsEmpty() {
        PluginFileRepository svc = new PluginFileRepository("/this/path/does/not/exist");
        List<ProjectRecord> records = List.of(
                ProjectRecord.forGitHub("medievalfactions", "Dans-Plugins", "Medieval-Factions")
        );
        assertTrue(svc.findAllConflictingJars(records).isEmpty());
    }

    @Test
    void findAllConflictingJars_emptyInputReturnsEmpty(@TempDir Path tempDir) throws IOException {
        createFile(tempDir, "Medieval-Factions-4.6.3.jar");
        PluginFileRepository svc = new PluginFileRepository(tempDir.toString());
        assertTrue(svc.findAllConflictingJars(List.of()).isEmpty());
    }

    private void createFile(Path dir, String name) throws IOException {
        new File(dir.toFile(), name).createNewFile();
    }

    // -------------------------------------------------------------------------
    // Regression: jars that survived an install and broke the next restart (#130)
    // -------------------------------------------------------------------------

    @Test
    void findConflictingJars_reportsAJarDifferingOnlyInCase() throws IOException {
        // On a case-sensitive filesystem these are two different files. Skipping
        // the first as "the managed file" is what let it survive an install and
        // leave Bukkit with two jars for one plugin.
        createJar(tempDirOf("case"), "Herald.jar", "Herald");

        PluginFileRepository svc = new PluginFileRepository(tempDirOf("case").toString());
        ProjectRecord record = ProjectRecord.forGitHub("herald", "Dans-Plugins", "Herald");

        List<File> conflicts = svc.findConflictingJars(record);
        assertEquals(1, conflicts.size());
        assertEquals("Herald.jar", conflicts.get(0).getName());
    }

    @Test
    void findConflictingJars_reportsANonVersionBuildQualifier() throws IOException {
        // "-ci-build" is not a version, so the filename heuristic normalises this
        // to "wildpetscibuild" and misses it. The jar's own plugin.yml does not.
        createJar(tempDirOf("qualifier"), "WildPets-ci-build.jar", "WildPets");

        PluginFileRepository svc = new PluginFileRepository(tempDirOf("qualifier").toString());
        ProjectRecord record = ProjectRecord.forGitHub("wildpets", "Dans-Plugins", "Wild-Pets");

        List<File> conflicts = svc.findConflictingJars(record);
        assertEquals(1, conflicts.size());
        assertEquals("WildPets-ci-build.jar", conflicts.get(0).getName());
    }

    @Test
    void findConflictingJars_stillIgnoresTheExactManagedFile() throws IOException {
        createJar(tempDirOf("managed"), "herald.jar", "Herald");

        PluginFileRepository svc = new PluginFileRepository(tempDirOf("managed").toString());
        ProjectRecord record = ProjectRecord.forGitHub("herald", "Dans-Plugins", "Herald");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_doesNotClaimAnUnrelatedPluginByItsDeclaredName() throws IOException {
        createJar(tempDirOf("unrelated"), "SomeOtherPlugin.jar", "SomeOtherPlugin");

        PluginFileRepository svc = new PluginFileRepository(tempDirOf("unrelated").toString());
        ProjectRecord record = ProjectRecord.forGitHub("herald", "Dans-Plugins", "Herald");

        assertTrue(svc.findConflictingJars(record).isEmpty());
    }

    @Test
    void findConflictingJars_fallsBackToTheFilenameWhenAJarIsUnreadable() throws IOException {
        // Not a zip at all. An install must not fail because something odd is
        // sitting in the plugins folder.
        Path dir = tempDirOf("unreadable");
        java.nio.file.Files.write(dir.resolve("Herald-1.2.3.jar"), "not a zip".getBytes());

        PluginFileRepository svc = new PluginFileRepository(dir.toString());
        ProjectRecord record = ProjectRecord.forGitHub("herald", "Dans-Plugins", "Herald");

        List<File> conflicts = svc.findConflictingJars(record);
        assertEquals(1, conflicts.size());
        assertEquals("Herald-1.2.3.jar", conflicts.get(0).getName());
    }

    @Test
    void readDeclaredPluginName_ignoresAnIndentedNameKey() throws IOException {
        // `name:` inside a commands: block belongs to a command, not the plugin.
        Path dir = tempDirOf("indented");
        createJarWithYaml(dir, "Thing.jar", "commands:\n  foo:\n    name: NotThePlugin\nname: Thing\n");

        PluginFileRepository svc = new PluginFileRepository(dir.toString());
        assertEquals("Thing", svc.readDeclaredPluginName(dir.resolve("Thing.jar").toFile()));
    }

    @Test
    void readDeclaredPluginName_returnsNullWhenThereIsNoPluginYml() throws IOException {
        Path dir = tempDirOf("noyml");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                java.nio.file.Files.newOutputStream(dir.resolve("Empty.jar")))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("nothing.txt"));
            zos.write("x".getBytes());
            zos.closeEntry();
        }
        PluginFileRepository svc = new PluginFileRepository(dir.toString());
        assertNull(svc.readDeclaredPluginName(dir.resolve("Empty.jar").toFile()));
    }

    // Each regression test gets its own directory so one cannot see another's jars.
    private Path tempDirOf(String key) throws IOException {
        Path dir = regressionRoot.resolve(key);
        if (!java.nio.file.Files.exists(dir)) java.nio.file.Files.createDirectories(dir);
        return dir;
    }

    private void createJar(Path dir, String filename, String declaredName) throws IOException {
        createJarWithYaml(dir, filename, "name: " + declaredName + "\nversion: 1.0\nmain: a.B\n");
    }

    private void createJarWithYaml(Path dir, String filename, String yaml) throws IOException {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                java.nio.file.Files.newOutputStream(dir.resolve(filename)))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("plugin.yml"));
            zos.write(yaml.getBytes());
            zos.closeEntry();
        }
    }

    @TempDir
    Path regressionRoot;

}
