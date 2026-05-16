package dansplugins.dpm;

import dansplugins.dpm.utils.TabCompleter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TabCompletionTest {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("help", "list", "get", "clean", "stats", "update");

    // -------------------------------------------------------------------------
    // sub-command completion
    // -------------------------------------------------------------------------

    @Test
    void filterByPrefix_emptyPartialReturnsAll() {
        assertEquals(SUBCOMMANDS, TabCompleter.filterByPrefix(SUBCOMMANDS, ""));
    }

    @Test
    void filterByPrefix_exactMatchReturnsOne() {
        assertEquals(List.of("get"), TabCompleter.filterByPrefix(SUBCOMMANDS, "get"));
    }

    @Test
    void filterByPrefix_prefixMatchReturnsSubset() {
        assertEquals(List.of("list"), TabCompleter.filterByPrefix(SUBCOMMANDS, "l"));
    }

    @Test
    void filterByPrefix_caseInsensitiveMatch() {
        assertEquals(List.of("get"), TabCompleter.filterByPrefix(SUBCOMMANDS, "GET"));
        assertEquals(List.of("clean"), TabCompleter.filterByPrefix(SUBCOMMANDS, "CL"));
    }

    @Test
    void filterByPrefix_noMatchReturnsEmpty() {
        assertTrue(TabCompleter.filterByPrefix(SUBCOMMANDS, "xyz").isEmpty());
    }

    @Test
    void filterByPrefix_updatePrefixMatchesOnlyUpdate() {
        assertEquals(List.of("update"), TabCompleter.filterByPrefix(SUBCOMMANDS, "u"));
    }

    // -------------------------------------------------------------------------
    // plugin-name completion
    // -------------------------------------------------------------------------

    @Test
    void filterByPrefix_partialPluginNameNarrowsResults() {
        List<String> plugins = Arrays.asList(
                "medievalfactions", "medievaleconomy", "medievalroleplayengine",
                "currencies", "simpleskills"
        );
        List<String> result = TabCompleter.filterByPrefix(plugins, "medieval");
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(
                "medievalfactions", "medievaleconomy", "medievalroleplayengine")));
    }

    @Test
    void filterByPrefix_singleCharacterNarrows() {
        List<String> plugins = Arrays.asList("currencies", "conquestrecipes", "simpleskills");
        assertEquals(List.of("currencies", "conquestrecipes"),
                TabCompleter.filterByPrefix(plugins, "c"));
    }
}
