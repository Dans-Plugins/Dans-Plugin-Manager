package dansplugins.dpm.install.ports;

import dansplugins.dpm.objects.ProjectRecord;

import java.util.List;

/**
 * The server's plugins folder, as the install pipeline sees it.
 *
 * <p>A plugin counts as installed when its managed jar, {@code <Name>.jar}, is present (matched
 * ignoring case). {@link #isInstalled} scans the folder on every call, so never call it in a loop:
 * use {@link #filterInstalled}, which scans once for any number of records. Reads never throw; an
 * unreadable folder looks empty.
 */
public interface PluginFolder {
    boolean isInstalled(ProjectRecord record);

    /** The records whose managed jar is present, in input order, from a single folder scan. */
    List<ProjectRecord> filterInstalled(List<ProjectRecord> records);

    /** The path the managed jar for this record is written to. */
    String managedJarPath(ProjectRecord record);

    /**
     * File names of other jars in the folder that hold the same plugin under a different name,
     * which Bukkit would refuse to load alongside the managed jar.
     */
    List<String> conflictingJars(ProjectRecord record);

    /** Deletes the named jar from the folder; false if it could not be deleted. */
    boolean deleteJar(String fileName);
}
