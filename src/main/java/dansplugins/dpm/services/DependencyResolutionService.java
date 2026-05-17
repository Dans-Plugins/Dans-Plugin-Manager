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
     * Returns names of installed plugins that declare a hard dependency on {@code targetName}.
     * Uses a single filterInstalled() scan regardless of registry size.
     */
    public List<String> findDependents(String targetName, List<ProjectRecord> installedRecords) {
        String targetLower = targetName.toLowerCase();
        List<String> dependents = new ArrayList<>();
        for (ProjectRecord installed : installedRecords) {
            for (String dep : installed.getHardDependencies()) {
                if (dep.equalsIgnoreCase(targetLower)) {
                    dependents.add(installed.getName());
                    break;
                }
            }
        }
        return dependents;
    }

    // resolved must be pre-seeded with lowercase names already in the batch; prevents circular re-processing
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
