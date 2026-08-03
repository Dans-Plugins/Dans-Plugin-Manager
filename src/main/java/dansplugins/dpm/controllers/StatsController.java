package dansplugins.dpm.controllers;

import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;

// Aggregates /dpm stats counts, returning plain result data rather than sending messages.
// StatsCommand formats and sends the results.
public class StatsController {

    public static final class Stats {
        private final int total;
        private final int installed;
        private final int available;

        Stats(int total, int installed, int available) {
            this.total = total;
            this.installed = installed;
            this.available = available;
        }

        public int getTotal() { return total; }
        public int getInstalled() { return installed; }
        public int getAvailable() { return available; }
    }

    private final ProjectRecordRepository projectRecordRepository;
    private final PluginFileRepository pluginFileRepository;

    public StatsController(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository) {
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFileRepository = pluginFileRepository;
    }

    public Stats getStats() {
        int total = projectRecordRepository.getNumProjectRecords();
        int installed = pluginFileRepository.filterInstalled(projectRecordRepository.getAllProjectRecords()).size();
        int available = total - installed;
        return new Stats(total, installed, available);
    }
}
