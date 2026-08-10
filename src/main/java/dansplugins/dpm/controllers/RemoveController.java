package dansplugins.dpm.controllers;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.services.DependencyResolutionService;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

// Orchestrates /dpm remove: dependent lookup, file deletion, and version-store cleanup,
// returning plain result data rather than sending messages. RemoveCommand formats and sends
// the results.
public class RemoveController {

    public enum Outcome { NOT_INSTALLED, DELETED, DELETE_FAILED }

    public static final class RemovalPreview {
        private final File jar;
        private final List<String> dependents;

        RemovalPreview(File jar, List<String> dependents) {
            this.jar = jar;
            this.dependents = dependents;
        }

        public File getJar() { return jar; }
        public List<String> getDependents() { return dependents; }
        public boolean isInstalled() { return jar != null; }
    }

    public static final class RemovalResult {
        private final Outcome outcome;
        private final File jar;
        private final List<String> dependents;

        RemovalResult(Outcome outcome, File jar, List<String> dependents) {
            this.outcome = outcome;
            this.jar = jar;
            this.dependents = dependents;
        }

        public Outcome getOutcome() { return outcome; }
        public File getJar() { return jar; }
        public List<String> getDependents() { return dependents; }
    }

    private final ProjectRecordRepository projectRecordRepository;
    private final PluginFileRepository pluginFileRepository;
    private final VersionRepository versionRepository;
    private final ChannelRepository channelRepository;
    private final DependencyResolutionService dependencyResolutionService;
    private final Logger logger;

    public RemoveController(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository,
                            VersionRepository versionRepository, ChannelRepository channelRepository,
                            DependencyResolutionService dependencyResolutionService, Logger logger) {
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.versionRepository = versionRepository;
        this.channelRepository = channelRepository;
        this.dependencyResolutionService = dependencyResolutionService;
        this.logger = logger;
    }

    public RemovalPreview preview(ProjectRecord record) {
        File jar = pluginFileRepository.getInstalledFile(record);
        if (jar == null) {
            return new RemovalPreview(null, List.of());
        }
        List<ProjectRecord> installed = pluginFileRepository.filterInstalled(projectRecordRepository.getAllProjectRecords());
        List<String> dependents = dependencyResolutionService.findDependents(record.getName(), installed);
        return new RemovalPreview(jar, dependents);
    }

    public RemovalResult remove(ProjectRecord record) {
        RemovalPreview preview = preview(record);
        if (!preview.isInstalled()) {
            return new RemovalResult(Outcome.NOT_INSTALLED, null, List.of());
        }
        File jar = preview.getJar();
        if (jar.delete()) {
            versionRepository.removeTag(record.getName());
            channelRepository.removeChannel(record.getName());
            logger.info("[DPM] Removed " + record.getName() + ".");
            return new RemovalResult(Outcome.DELETED, jar, preview.getDependents());
        }
        logger.warning("[DPM] Failed to delete " + jar.getName() + " — check server file permissions.");
        return new RemovalResult(Outcome.DELETE_FAILED, jar, preview.getDependents());
    }

    public List<ProjectRecord> getInstalledPlugins() {
        return pluginFileRepository.filterInstalled(projectRecordRepository.getAllProjectRecords());
    }
}
