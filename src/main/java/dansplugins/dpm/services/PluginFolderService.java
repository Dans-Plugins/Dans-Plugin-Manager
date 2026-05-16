package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PluginFolderService {
    private final String pluginsFolder;

    public PluginFolderService() {
        this("./plugins/");
    }

    PluginFolderService(String pluginsFolder) {
        this.pluginsFolder = pluginsFolder;
    }

    /** Returns true if the managed JAR ({recordName}.jar) exists in the plugins folder. */
    public boolean isInstalled(ProjectRecord record) {
        return new File(pluginsFolder, record.getName() + ".jar").exists();
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
