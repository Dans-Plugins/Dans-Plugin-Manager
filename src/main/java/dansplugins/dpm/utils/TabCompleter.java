package dansplugins.dpm.utils;

import java.util.ArrayList;
import java.util.List;

public class TabCompleter {

    private TabCompleter() {}

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
