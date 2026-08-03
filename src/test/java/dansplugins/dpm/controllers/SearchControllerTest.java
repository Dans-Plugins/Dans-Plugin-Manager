package dansplugins.dpm.controllers;

import dansplugins.dpm.controllers.SearchController.SearchResult;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchControllerTest {

    private static ProjectRecord record(String name) {
        return ProjectRecord.forGitHub(name, "Dans-Plugins", name);
    }

    private static ProjectRecordRepository projectRecordRepository(ProjectRecord... records) {
        ProjectRecordRepository repository = new ProjectRecordRepository();
        for (ProjectRecord record : records) {
            repository.addProjectRecord(record);
        }
        return repository;
    }

    private static VersionRepository versionRepository(Path tempDir) {
        return new VersionRepository(new File(tempDir.toFile(), "dpm-versions.properties"), null);
    }

    // -------------------------------------------------------------------------
    // matchesKeyword()
    // -------------------------------------------------------------------------

    @Test
    void matchesKeyword_matchesNameSubstring() {
        ProjectRecord record = ProjectRecord.forGitHub("MedievalFactions", "Dans-Plugins", "MedievalFactions");
        assertTrue(SearchController.matchesKeyword(record, "medieval"));
    }

    @Test
    void matchesKeyword_matchesNameCaseInsensitive() {
        ProjectRecord record = ProjectRecord.forGitHub("MedievalFactions", "Dans-Plugins", "MedievalFactions");
        assertTrue(SearchController.matchesKeyword(record, "MEDIEVAL"));
    }

    @Test
    void matchesKeyword_matchesDescriptionSubstring() {
        ProjectRecord record = ProjectRecord.builder("MyPlugin", "org", "MyPlugin")
                .description("A land claiming and faction system")
                .build();
        assertTrue(SearchController.matchesKeyword(record, "faction"));
    }

    @Test
    void matchesKeyword_matchesDescriptionCaseInsensitive() {
        ProjectRecord record = ProjectRecord.builder("MyPlugin", "org", "MyPlugin")
                .description("A Land Claiming System")
                .build();
        assertTrue(SearchController.matchesKeyword(record, "land claiming"));
    }

    @Test
    void matchesKeyword_returnsFalseWhenNoMatch() {
        ProjectRecord record = ProjectRecord.builder("MyPlugin", "org", "MyPlugin")
                .description("A land claiming system")
                .build();
        assertFalse(SearchController.matchesKeyword(record, "economy"));
    }

    @Test
    void matchesKeyword_returnsFalseWhenDescriptionNullAndNameNoMatch() {
        ProjectRecord record = ProjectRecord.forGitHub("MyPlugin", "org", "MyPlugin");
        assertFalse(SearchController.matchesKeyword(record, "economy"));
    }

    @Test
    void matchesKeyword_nullDescriptionDoesNotThrow() {
        ProjectRecord record = ProjectRecord.forGitHub("MyPlugin", "org", "MyPlugin");
        assertDoesNotThrow(() -> SearchController.matchesKeyword(record, "anything"));
    }

    @Test
    void matchesKeyword_emptyKeywordMatchesAll() {
        ProjectRecord record = ProjectRecord.forGitHub("AnyPlugin", "org", "AnyPlugin");
        assertTrue(SearchController.matchesKeyword(record, ""));
    }

    @Test
    void matchesKeyword_multiWordKeywordMatchesDescription() {
        ProjectRecord record = ProjectRecord.builder("MyPlugin", "org", "MyPlugin")
                .description("Custom spawn point management")
                .build();
        assertTrue(SearchController.matchesKeyword(record, "spawn point"));
    }

    // -------------------------------------------------------------------------
    // search()
    // -------------------------------------------------------------------------

    @Test
    void search_returnsEmptyListWhenNoMatches(@TempDir Path tempDir) {
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(record("MedievalFactions"));
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        SearchController controller = new SearchController(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        List<SearchResult> results = controller.search("economy");

        assertTrue(results.isEmpty());
    }

    @Test
    void search_marksMatchesNotInstalledWithNoStoredTag(@TempDir Path tempDir) {
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(record("MedievalFactions"));
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        SearchController controller = new SearchController(projectRecordRepository, pluginFileRepository, versionRepository(tempDir));

        List<SearchResult> results = controller.search("medieval");

        assertEquals(1, results.size());
        assertFalse(results.get(0).isInstalled());
        assertNull(results.get(0).getStoredTag());
    }

    @Test
    void search_marksMatchesInstalledWithStoredTag(@TempDir Path tempDir) throws Exception {
        ProjectRecord installed = record("MedievalFactions");
        new File(tempDir.toFile(), "MedievalFactions.jar").createNewFile();
        ProjectRecordRepository projectRecordRepository = projectRecordRepository(installed);
        PluginFileRepository pluginFileRepository = new PluginFileRepository(tempDir.toString());
        VersionRepository versionRepository = versionRepository(tempDir);
        versionRepository.setTag("MedievalFactions", "v1.0.0");
        SearchController controller = new SearchController(projectRecordRepository, pluginFileRepository, versionRepository);

        List<SearchResult> results = controller.search("medieval");

        assertEquals(1, results.size());
        assertTrue(results.get(0).isInstalled());
        assertEquals("v1.0.0", results.get(0).getStoredTag());
    }
}
