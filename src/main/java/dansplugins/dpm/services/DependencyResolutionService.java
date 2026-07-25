package dansplugins.dpm.services;

import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.objects.ProjectRecord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyResolutionService {
    private final ProjectRecordRepository projectRecordRepository;
    private final PluginFolderService pluginFolderService;

    public DependencyResolutionService(ProjectRecordRepository projectRecordRepository, PluginFolderService pluginFolderService) {
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFolderService = pluginFolderService;
    }

    // Caller supplies the pre-filtered installed list so no extra directory scan happens here.
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
        Set<String> installedLower = pluginFolderService.filterInstalled(projectRecordRepository.getAllProjectRecords())
                .stream().map(r -> r.getName().toLowerCase()).collect(Collectors.toSet());

        Queue<ProjectRecord> queue = new ArrayDeque<>(toProcess);
        while (!queue.isEmpty()) {
            ProjectRecord record = queue.poll();
            for (String dep : record.getHardDependencies()) {
                String depLower = dep.toLowerCase();
                if (resolved.contains(depLower)) continue;
                resolved.add(depLower);
                if (installedLower.contains(depLower)) continue;
                ProjectRecord depRecord = projectRecordRepository.getProjectRecord(dep);
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
