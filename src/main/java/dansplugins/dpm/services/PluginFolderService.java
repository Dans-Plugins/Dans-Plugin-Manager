package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PluginFolderService {
    private final String pluginsFolder;

    public PluginFolderService() {
        this("./plugins/");
    }

    PluginFolderService(String pluginsFolder) {
        this.pluginsFolder = pluginsFolder;
    }

    String getPluginsFolder() {
        return pluginsFolder;
    }

    public boolean isInstalled(ProjectRecord record) {
        return getInstalledFile(record) != null;
    }

    /**
     * Returns the installed JAR file for the record, or null if not found.
     * Case-insensitive — all isInstalled() calls delegate here.
     */
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

    /**
     * Returns the subset of records whose managed JAR is present in the plugins folder.
     * Scans the directory once regardless of how many records are provided.
     */
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

    /**
     * Returns all JARs in the plugins folder whose normalized name matches the record
     * but are not the managed file ({recordName}.jar).
     */
    public List<File> findConflictingJars(ProjectRecord record) {
        List<File> conflicts = new ArrayList<>();
        File pluginsDir = new File(pluginsFolder);
        File[] jars = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null) return conflicts;
        String managedFilename = record.getName() + ".jar";
        for (File jar : jars) {
            if (jar.getName().equalsIgnoreCase(managedFilename)) continue;
            if (normalize(jar.getName()).equals(record.getName())) {
                conflicts.add(jar);
            }
        }
        return conflicts;
    }

    /**
     * Normalizes a JAR filename for comparison against a record name:
     * strips the .jar extension, removes any trailing version suffix
     * (e.g. -4.6.3, -v1.0), strips hyphens and underscores, lowercases.
     *
     * Examples:
     *   Medieval-Factions-4.6.3.jar  -> medievalfactions
     *   ActivityTracker-v1.0.jar     -> activitytracker
     *   Bluemap_MedievalFactions.jar -> bluemapmedievalfactions
     */
    String normalize(String filename) {
        String name = filename.replaceAll("(?i)\\.jar$", "");
        name = name.replaceAll("[-_]v?\\d.*$", "");
        name = name.replaceAll("[-_]", "");
        return name.toLowerCase();
    }
}
