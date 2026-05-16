package dansplugins.dpm.utils;

import java.util.ArrayList;
import java.util.List;

public class TabCompleter {

    private TabCompleter() {}

    /**
     * Returns every option whose lowercase form starts with the lowercase form
     * of {@code partial}, preserving original casing in the returned strings.
     */
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
