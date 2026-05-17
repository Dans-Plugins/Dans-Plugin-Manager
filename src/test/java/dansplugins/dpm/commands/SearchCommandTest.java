package dansplugins.dpm.commands;

import dansplugins.dpm.objects.ProjectRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchCommandTest {

    // -------------------------------------------------------------------------
    // matchesKeyword()
    // -------------------------------------------------------------------------

    @Test
    void matchesKeyword_matchesNameSubstring() {
        ProjectRecord record = ProjectRecord.forGitHub("MedievalFactions", "Dans-Plugins", "MedievalFactions");
        assertTrue(SearchCommand.matchesKeyword(record, "medieval"));
    }

    @Test
    void matchesKeyword_matchesNameCaseInsensitive() {
        ProjectRecord record = ProjectRecord.forGitHub("MedievalFactions", "Dans-Plugins", "MedievalFactions");
        assertTrue(SearchCommand.matchesKeyword(record, "MEDIEVAL"));
    }

    @Test
    void matchesKeyword_matchesDescriptionSubstring() {
        ProjectRecord record = ProjectRecord.builder("MyPlugin", "org", "MyPlugin")
                .description("A land claiming and faction system")
                .build();
        assertTrue(SearchCommand.matchesKeyword(record, "faction"));
    }

    @Test
    void matchesKeyword_matchesDescriptionCaseInsensitive() {
        ProjectRecord record = ProjectRecord.builder("MyPlugin", "org", "MyPlugin")
                .description("A Land Claiming System")
                .build();
        assertTrue(SearchCommand.matchesKeyword(record, "land claiming"));
    }

    @Test
    void matchesKeyword_returnsFalseWhenNoMatch() {
        ProjectRecord record = ProjectRecord.builder("MyPlugin", "org", "MyPlugin")
                .description("A land claiming system")
                .build();
        assertFalse(SearchCommand.matchesKeyword(record, "economy"));
    }

    @Test
    void matchesKeyword_returnsFalseWhenDescriptionNullAndNameNoMatch() {
        ProjectRecord record = ProjectRecord.forGitHub("MyPlugin", "org", "MyPlugin");
        assertFalse(SearchCommand.matchesKeyword(record, "economy"));
    }

    @Test
    void matchesKeyword_nullDescriptionDoesNotThrow() {
        ProjectRecord record = ProjectRecord.forGitHub("MyPlugin", "org", "MyPlugin");
        assertDoesNotThrow(() -> SearchCommand.matchesKeyword(record, "anything"));
    }

    @Test
    void matchesKeyword_emptyKeywordMatchesAll() {
        ProjectRecord record = ProjectRecord.forGitHub("AnyPlugin", "org", "AnyPlugin");
        assertTrue(SearchCommand.matchesKeyword(record, ""));
    }

    @Test
    void matchesKeyword_multiWordKeywordMatchesDescription() {
        ProjectRecord record = ProjectRecord.builder("MyPlugin", "org", "MyPlugin")
                .description("Custom spawn point management")
                .build();
        assertTrue(SearchCommand.matchesKeyword(record, "spawn point"));
    }
}
