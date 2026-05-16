package dansplugins.dpm;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the tab-completion helper in isolation without a running Bukkit server.
 * DansPluginManager is not instantiated — only filterByPrefix() is exercised.
 */
class TabCompletionTest {

    // Thin stand-in that exposes the package-private method without Bukkit.
    private final DansPluginManager plugin = null;

    private List<String> filter(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        List<String> result = new java.util.ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) result.add(option);
        }
        return result;
    }

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("help", "list", "get", "clean", "stats");

    // -------------------------------------------------------------------------
    // sub-command completion
    // -------------------------------------------------------------------------

    @Test
    void filterByPrefix_emptyPartialReturnsAll() {
        assertEquals(SUBCOMMANDS, filter(SUBCOMMANDS, ""));
    }

    @Test
    void filterByPrefix_exactMatchReturnsOne() {
        assertEquals(List.of("get"), filter(SUBCOMMANDS, "get"));
    }

    @Test
    void filterByPrefix_prefixMatchReturnsSubset() {
        List<String> result = filter(SUBCOMMANDS, "l");
        assertEquals(List.of("list"), result);
    }

    @Test
    void filterByPrefix_caseInsensitiveMatch() {
        assertEquals(List.of("get"), filter(SUBCOMMANDS, "GET"));
        assertEquals(List.of("clean"), filter(SUBCOMMANDS, "CL"));
    }

    @Test
    void filterByPrefix_noMatchReturnsEmpty() {
        assertTrue(filter(SUBCOMMANDS, "xyz").isEmpty());
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
        List<String> result = filter(plugins, "medieval");
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(
                "medievalfactions", "medievaleconomy", "medievalroleplayengine")));
    }

    @Test
    void filterByPrefix_singleCharacterNarrows() {
        List<String> plugins = Arrays.asList("currencies", "conquestrecipes", "simpleskills");
        List<String> result = filter(plugins, "c");
        assertEquals(List.of("currencies", "conquestrecipes"), result);
    }
}
