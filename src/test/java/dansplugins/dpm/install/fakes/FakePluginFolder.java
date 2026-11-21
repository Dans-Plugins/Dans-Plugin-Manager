package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.PluginFolder;
import dansplugins.dpm.objects.ProjectRecord;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A plugins folder held as a set of jar file names. Conflicts use the filename heuristic only. */
public class FakePluginFolder implements PluginFolder {
    private final String root;
    public final Set<String> jars = new LinkedHashSet<>();
    public final Set<String> undeletable = new LinkedHashSet<>();
    public int scans;

    public FakePluginFolder(String root) {
        this.root = root;
    }

    public FakePluginFolder with(String... fileNames) {
        for (String name : fileNames) jars.add(name);
        return this;
    }

    @Override
    public boolean isInstalled(ProjectRecord record) {
        scans++;
        return hasManaged(record);
    }

    @Override
    public List<ProjectRecord> filterInstalled(List<ProjectRecord> records) {
        scans++;
        List<ProjectRecord> installed = new ArrayList<>();
        for (ProjectRecord record : records) {
            if (hasManaged(record)) installed.add(record);
        }
        return installed;
    }

    @Override
    public String managedJarPath(ProjectRecord record) {
        return root + record.getName() + ".jar";
    }

    @Override
    public List<String> conflictingJars(ProjectRecord record) {
        List<String> conflicts = new ArrayList<>();
        for (String jar : jars) {
            if (jar.equals(record.getName() + ".jar")) continue;
            if (normalize(jar).equals(normalize(record.getName()))) conflicts.add(jar);
        }
        return conflicts;
    }

    @Override
    public boolean deleteJar(String fileName) {
        if (undeletable.contains(fileName)) return false;
        return jars.remove(fileName);
    }

    private boolean hasManaged(ProjectRecord record) {
        for (String jar : jars) {
            if (jar.equalsIgnoreCase(record.getName() + ".jar")) return true;
        }
        return false;
    }

    private static String normalize(String filename) {
        String name = filename.replaceAll("(?i)\\.jar$", "");
        name = name.replaceAll("[-_]v?\\d.*$", "");
        name = name.replaceAll("[-_]", "");
        return name.toLowerCase();
    }
}
