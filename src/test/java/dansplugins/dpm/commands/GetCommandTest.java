package dansplugins.dpm.commands;

import dansplugins.dpm.commands.GetCommand.ParsedArgs;
import dansplugins.dpm.objects.ReleaseChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// GetCommand itself depends on Bukkit and cannot be constructed here; parseArgs is static and
// Bukkit-free, so the flag handling is covered directly.
class GetCommandTest {

    // -------------------------------------------------------------------------
    // parseArgs()
    // -------------------------------------------------------------------------

    @Test
    void parseArgs_withNoFlagsLeavesTheChannelUnrequested() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"Currencies", "Fiefs"});

        assertNull(parsed.error);
        assertNull(parsed.requestedChannel, "No flag means the plugin's stored channel is used");
        assertEquals(List.of("Currencies", "Fiefs"), parsed.names);
    }

    @Test
    void parseArgs_stripsTheExperimentalFlagFromTheNameList() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"Currencies", "--experimental"});

        assertNull(parsed.error);
        assertEquals(ReleaseChannel.EXPERIMENTAL, parsed.requestedChannel);
        assertEquals(List.of("Currencies"), parsed.names);
    }

    @Test
    void parseArgs_acceptsTheFlagBeforeTheNames() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"--experimental", "Currencies", "Fiefs"});

        assertNull(parsed.error);
        assertEquals(ReleaseChannel.EXPERIMENTAL, parsed.requestedChannel);
        assertEquals(List.of("Currencies", "Fiefs"), parsed.names);
    }

    @Test
    void parseArgs_acceptsTheFlagBetweenNames() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"Currencies", "--stable", "Fiefs"});

        assertNull(parsed.error);
        assertEquals(ReleaseChannel.STABLE, parsed.requestedChannel);
        assertEquals(List.of("Currencies", "Fiefs"), parsed.names);
    }

    @Test
    void parseArgs_isCaseInsensitiveForFlags() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"Currencies", "--Experimental"});

        assertNull(parsed.error);
        assertEquals(ReleaseChannel.EXPERIMENTAL, parsed.requestedChannel);
    }

    @Test
    void parseArgs_allowsTheSameFlagRepeated() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"--stable", "Currencies", "--stable"});

        assertNull(parsed.error);
        assertEquals(ReleaseChannel.STABLE, parsed.requestedChannel);
        assertEquals(List.of("Currencies"), parsed.names);
    }

    @Test
    void parseArgs_rejectsBothChannelFlagsTogether() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"Currencies", "--experimental", "--stable"});

        assertNotNull(parsed.error);
        assertTrue(parsed.error.contains("cannot be used together"));
    }

    @Test
    void parseArgs_rejectsAnUnknownFlag() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"Currencies", "--beta"});

        assertNotNull(parsed.error);
        assertTrue(parsed.error.contains("--beta"));
        assertTrue(parsed.error.contains("--experimental"));
    }

    @Test
    void parseArgs_withOnlyAFlagYieldsNoNames() {
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"--experimental"});

        assertNull(parsed.error);
        assertTrue(parsed.names.isEmpty(), "A bare flag must fall through to the usage message");
    }

    @Test
    void parseArgs_treatsASingleDashArgumentAsAPluginName() {
        // Only "--" prefixed arguments are options; a lone dash is passed through as a name so the
        // existing "plugin not found" message explains the problem.
        ParsedArgs parsed = GetCommand.parseArgs(new String[]{"-experimental"});

        assertNull(parsed.error);
        assertEquals(List.of("-experimental"), parsed.names);
    }
}
