package dansplugins.dpm;

import dansplugins.dpm.utils.TabCompleter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TabCompletionTest {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("help", "list", "get", "clean", "stats", "update", "info", "reload", "remove");

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

    @Test
    void filterByPrefix_infoPrefixMatchesOnlyInfo() {
        assertEquals(List.of("info"), TabCompleter.filterByPrefix(SUBCOMMANDS, "i"));
    }

    @Test
    void filterByPrefix_rePrefixMatchesReloadAndRemove() {
        List<String> result = TabCompleter.filterByPrefix(SUBCOMMANDS, "re");
        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of("reload", "remove")));
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

    // -------------------------------------------------------------------------
    // channel-flag completion for /dpm get
    // -------------------------------------------------------------------------

    @Test
    void pluginNamesWithChannelFlags_offersBothFlagsAlongsidePluginNames() {
        List<String> options = TabCompleter.pluginNamesWithChannelFlags(
                List.of("currencies"), new String[]{"get", ""});

        assertTrue(options.containsAll(List.of("currencies", "--experimental", "--stable")));
    }

    @Test
    void pluginNamesWithChannelFlags_completesAPartiallyTypedFlag() {
        List<String> options = TabCompleter.pluginNamesWithChannelFlags(
                List.of("currencies"), new String[]{"get", "currencies", "--e"});

        assertEquals(List.of("--experimental"), TabCompleter.filterByPrefix(options, "--e"));
    }

    @Test
    void pluginNamesWithChannelFlags_stopsOfferingFlagsOnceOneIsPresent() {
        List<String> options = TabCompleter.pluginNamesWithChannelFlags(
                List.of("currencies"), new String[]{"get", "currencies", "--experimental", ""});

        assertEquals(List.of("currencies"), options,
                "The flags are mutually exclusive, so neither should be suggested a second time");
    }

    @Test
    void pluginNamesWithChannelFlags_isCaseInsensitiveWhenDetectingAnExistingFlag() {
        List<String> options = TabCompleter.pluginNamesWithChannelFlags(
                List.of("currencies"), new String[]{"get", "currencies", "--STABLE", ""});

        assertEquals(List.of("currencies"), options);
    }
}
