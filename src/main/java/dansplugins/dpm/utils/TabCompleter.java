package dansplugins.dpm.utils;

import java.util.ArrayList;
import java.util.List;

public class TabCompleter {

    /** Channel flags accepted by /dpm get, offered alongside plugin names. */
    public static final List<String> CHANNEL_FLAGS = List.of("--experimental", "--stable");

    private TabCompleter() {}

    /**
     * Returns the plugin names plus the channel flags, for completing /dpm get arguments.
     * The flags are mutually exclusive, so once either one is on the command line neither is
     * suggested again.
     *
     * <p>The final element of {@code argsSoFar} is the token being typed rather than a token
     * already entered, so it is excluded from that check — otherwise finishing "--experimental"
     * would withdraw the very suggestion the player is completing.
     */
    public static List<String> pluginNamesWithChannelFlags(List<String> pluginNames, String[] argsSoFar) {
        List<String> options = new ArrayList<>(pluginNames);
        if (!containsChannelFlag(argsSoFar, 1)) {
            options.addAll(CHANNEL_FLAGS);
        }
        return options;
    }

    private static boolean containsChannelFlag(String[] values, int trailingToIgnore) {
        if (values == null) return false;
        for (int i = 0; i < values.length - trailingToIgnore; i++) {
            for (String flag : CHANNEL_FLAGS) {
                if (flag.equalsIgnoreCase(values[i])) return true;
            }
        }
        return false;
    }

    public static List<String> filterByPrefix(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
