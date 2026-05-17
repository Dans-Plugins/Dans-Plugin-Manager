package dansplugins.dpm.services;

import dansplugins.dpm.objects.ProjectRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public Map<String, List<File>> findAllConflictingJars(List<ProjectRecord> records) {
        Map<String, List<File>> result = new LinkedHashMap<>();
        File pluginsDir = new File(pluginsFolder);
        File[] jars = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null) return result;
        for (ProjectRecord record : records) {
            String managedFilename = record.getName() + ".jar";
            List<File> conflicts = new ArrayList<>();
            for (File jar : jars) {
                if (jar.getName().equalsIgnoreCase(managedFilename)) continue;
                if (normalize(jar.getName()).equals(record.getName())) {
                    conflicts.add(jar);
                }
            }
            if (!conflicts.isEmpty()) {
                result.put(record.getName(), conflicts);
            }
        }
        return result;
    }

    // strips .jar, trailing version suffix (-4.6.3, -v1.0), hyphens/underscores, lowercases
    String normalize(String filename) {
        String name = filename.replaceAll("(?i)\\.jar$", "");
        name = name.replaceAll("[-_]v?\\d.*$", "");
        name = name.replaceAll("[-_]", "");
        return name.toLowerCase();
    }
}
