package dansplugins.dpm.repositories;

import dansplugins.dpm.objects.ProjectRecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginFileRepository {
    private final String pluginsFolder;

    public PluginFileRepository() {
        this("./plugins/");
    }

    public PluginFileRepository(String pluginsFolder) {
        this.pluginsFolder = pluginsFolder;
    }

    public String getPluginsFolder() {
        return pluginsFolder;
    }

    public boolean isInstalled(ProjectRecord record) {
        return getInstalledFile(record) != null;
    }

    public File getInstalledFile(ProjectRecord record) {
        String managedFilename = record.getName() + ".jar";
        File pluginsDir = new File(pluginsFolder);
        File[] files = pluginsDir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().equalsIgnoreCase(managedFilename)) return f;
        }
        return null;
    }

    public List<ProjectRecord> filterInstalled(List<ProjectRecord> records) {
        File pluginsDir = new File(pluginsFolder);
        File[] files = pluginsDir.listFiles();
        if (files == null) return new ArrayList<>();
        Set<String> presentLower = new HashSet<>();
        for (File f : files) {
            presentLower.add(f.getName().toLowerCase());
        }
        List<ProjectRecord> installed = new ArrayList<>();
        for (ProjectRecord record : records) {
            if (presentLower.contains(record.getName().toLowerCase() + ".jar")) {
                installed.add(record);
            }
        }
        return installed;
    }

    public List<File> findConflictingJars(ProjectRecord record) {
        List<File> conflicts = new ArrayList<>();
        File[] jars = listJars();
        if (jars == null) return conflicts;
        for (File jar : jars) {
            if (isConflict(jar, record)) conflicts.add(jar);
        }
        return conflicts;
    }

    public Map<String, List<File>> findAllConflictingJars(List<ProjectRecord> records) {
        Map<String, List<File>> result = new LinkedHashMap<>();
        File[] jars = listJars();
        if (jars == null) return result;
        for (ProjectRecord record : records) {
            List<File> conflicts = new ArrayList<>();
            for (File jar : jars) {
                if (isConflict(jar, record)) conflicts.add(jar);
            }
            if (!conflicts.isEmpty()) {
                result.put(record.getName(), conflicts);
            }
        }
        return result;
    }

    private File[] listJars() {
        return new File(pluginsFolder).listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
    }

    /**
     * Whether {@code jar} holds the same plugin as {@code record} while sitting
     * under a different filename, so installing the record would leave two jars
     * providing one plugin — a state Bukkit refuses to load.
     *
     * The exclusion is an exact filename match, deliberately not a
     * case-insensitive one. On a case-sensitive filesystem `Herald.jar` and
     * `herald.jar` are two different files, and treating the first as "the
     * managed file" is what let it survive an install and break the next
     * restart. Where case does not distinguish files the two names are the same
     * file, which the install is about to replace anyway, so reporting it costs
     * nothing.
     */
    private boolean isConflict(File jar, ProjectRecord record) {
        String managedFilename = record.getName() + ".jar";
        if (jar.getName().equals(managedFilename)) return false;

        // What the jar says it is, which beats guessing from its filename.
        String declared = readDeclaredPluginName(jar);
        if (declared != null) return normalize(declared).equals(normalize(record.getName()));

        // Unreadable or no plugin.yml: fall back to the filename heuristic.
        return normalize(jar.getName()).equals(normalize(record.getName()));
    }

    /**
     * The `name:` a jar declares in its plugin.yml, or null if it has none that
     * can be read.
     *
     * Read with a line scan rather than a YAML parser: plugin.yml's `name` is a
     * plain scalar at the top level, Bukkit itself requires it, and a parser
     * would be a dependency for one field. A jar that is corrupt, is not a
     * plugin, or cannot be opened returns null so the caller falls back rather
     * than failing an install over it.
     */
    String readDeclaredPluginName(File jar) {
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry("plugin.yml");
            if (entry == null) return null;
            try (InputStream in = zip.getInputStream(entry);
                 Scanner scanner = new Scanner(in, "UTF-8")) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    // Top-level only: an indented `name:` belongs to a command
                    // or permission block, not to the plugin.
                    if (line.startsWith("name:")) {
                        String value = line.substring("name:".length()).trim();
                        if (value.startsWith("\"") || value.startsWith("'")) {
                            value = value.substring(1, Math.max(1, value.length() - 1));
                        }
                        return value.isEmpty() ? null : value;
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
        return null;
    }

    // strips .jar, trailing version suffix (-4.6.3, -v1.0), hyphens/underscores, lowercases
    String normalize(String filename) {
        String name = filename.replaceAll("(?i)\\.jar$", "");
        name = name.replaceAll("[-_]v?\\d.*$", "");
        name = name.replaceAll("[-_]", "");
        return name.toLowerCase();
    }
}
