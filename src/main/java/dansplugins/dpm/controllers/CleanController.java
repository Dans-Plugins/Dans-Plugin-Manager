package dansplugins.dpm.controllers;

import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// Orchestrates /dpm clean: duplicate-JAR detection and removal, returning plain result data
// rather than sending messages. CleanCommand formats and sends the results.
public class CleanController {

    // A duplicate JAR paired with the managed plugin it duplicates, so the command can label it
    // without re-deriving which record the file belongs to.
    public static final class Conflict {
        private final String pluginName;
        private final File jar;

        Conflict(String pluginName, File jar) {
            this.pluginName = pluginName;
            this.jar = jar;
        }

        public String getPluginName() { return pluginName; }
        public File getJar() { return jar; }
    }

    public static final class CleanResult {
        private final List<Conflict> removed;
        private final List<Conflict> failed;

        CleanResult(List<Conflict> removed, List<Conflict> failed) {
            this.removed = removed;
            this.failed = failed;
        }

        public List<Conflict> getRemoved() { return removed; }
        public List<Conflict> getFailed() { return failed; }
        public boolean isEmpty() { return removed.isEmpty() && failed.isEmpty(); }
    }

    private final ProjectRecordRepository projectRecordRepository;
    private final PluginFileRepository pluginFileRepository;
    private final Logger logger;

    public CleanController(ProjectRecordRepository projectRecordRepository,
                           PluginFileRepository pluginFileRepository, Logger logger) {
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.logger = logger;
    }

    public List<Conflict> findConflicts() {
        Map<String, List<File>> conflictMap =
                pluginFileRepository.findAllConflictingJars(projectRecordRepository.getAllProjectRecords());
        List<Conflict> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<File>> entry : conflictMap.entrySet()) {
            for (File jar : entry.getValue()) {
                conflicts.add(new Conflict(entry.getKey(), jar));
            }
        }
        return conflicts;
    }

    public CleanResult clean() {
        List<Conflict> removed = new ArrayList<>();
        List<Conflict> failed = new ArrayList<>();
        for (Conflict conflict : findConflicts()) {
            String label = conflict.getJar().getName() + " (" + conflict.getPluginName() + ")";
            if (conflict.getJar().delete()) {
                logger.info("[DPM] Cleaned duplicate JAR: " + label);
                removed.add(conflict);
            } else {
                logger.warning("[DPM] Failed to delete duplicate JAR: " + label + " — check file permissions.");
                failed.add(conflict);
            }
        }
        return new CleanResult(removed, failed);
    }
}
