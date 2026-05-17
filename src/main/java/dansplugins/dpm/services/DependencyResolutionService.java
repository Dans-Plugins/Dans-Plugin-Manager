package dansplugins.dpm.services;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyResolutionService {
    private final EphemeralData ephemeralData;
    private final PluginFolderService pluginFolderService;

    public DependencyResolutionService(EphemeralData ephemeralData, PluginFolderService pluginFolderService) {
        this.ephemeralData = ephemeralData;
        this.pluginFolderService = pluginFolderService;
    }

    /**
     * Resolves all missing hard dependencies for the given records, recursively.
     *
     * Populates depsToFetch with DPC plugins that need to be downloaded, and unknownDeps
     * with dependency names that are not registered DPC plugins (cannot be auto-downloaded).
     *
     * The resolved set must be pre-populated with the lowercase names of records already
     * in the download batch — this prevents re-processing and handles circular chains.
     *
     * The directory is scanned once regardless of how many records are processed.
     */
    public void resolve(List<ProjectRecord> toProcess, Set<String> resolved,
                        List<ProjectRecord> depsToFetch, List<String> unknownDeps) {
        Set<String> installedLower = pluginFolderService.filterInstalled(ephemeralData.getAllProjectRecords())
                .stream().map(r -> r.getName().toLowerCase()).collect(Collectors.toSet());

        Queue<ProjectRecord> queue = new ArrayDeque<>(toProcess);
        while (!queue.isEmpty()) {
            ProjectRecord record = queue.poll();
            for (String dep : record.getHardDependencies()) {
                String depLower = dep.toLowerCase();
                if (resolved.contains(depLower)) continue;
                resolved.add(depLower);
                if (installedLower.contains(depLower)) continue;
                ProjectRecord depRecord = ephemeralData.getProjectRecord(dep);
                if (depRecord == null) {
                    unknownDeps.add(dep);
                } else {
                    depsToFetch.add(depRecord);
                    queue.add(depRecord);
                }
            }
        }
    }
}
