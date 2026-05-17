package dansplugins.dpm.services;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyResolutionServiceTest {

    @TempDir
    Path tempDir;

    private EphemeralData ephemeralData;
    private PluginFolderService pluginFolderService;
    private DependencyResolutionService service;

    @BeforeEach
    void setUp() {
        ephemeralData = new EphemeralData();
        pluginFolderService = new PluginFolderService(tempDir.toString());
        service = new DependencyResolutionService(ephemeralData, pluginFolderService);
    }

    private ProjectRecord registerRecord(String name, List<String> hardDeps) {
        ProjectRecord record = ProjectRecord.builder(name, "owner", "repo")
                .hardDependencies(hardDeps)
                .build();
        ephemeralData.addProjectRecord(record);
        return record;
    }

    private void installJar(String name) throws IOException {
        Files.createFile(tempDir.resolve(name + ".jar"));
    }

    // -------------------------------------------------------------------------
    // resolve()
    // -------------------------------------------------------------------------

    @Test
    void resolve_returnsEmptyWhenNoDependencies() {
        ProjectRecord a = registerRecord("pluginA", List.of());
        List<ProjectRecord> depsToFetch = new ArrayList<>();
        List<String> unknownDeps = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        resolved.add("plugina");

        service.resolve(List.of(a), resolved, depsToFetch, unknownDeps);

        assertTrue(depsToFetch.isEmpty());
        assertTrue(unknownDeps.isEmpty());
    }

    @Test
    void resolve_fetchesMissingKnownDependency() {
        ProjectRecord dep = registerRecord("depPlugin", List.of());
        ProjectRecord main = registerRecord("mainPlugin", List.of("depPlugin"));
        List<ProjectRecord> depsToFetch = new ArrayList<>();
        List<String> unknownDeps = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        resolved.add("mainplugin");

        service.resolve(List.of(main), resolved, depsToFetch, unknownDeps);

        assertEquals(1, depsToFetch.size());
        assertEquals("depPlugin", depsToFetch.get(0).getName());
        assertTrue(unknownDeps.isEmpty());
    }

    @Test
    void resolve_addsToUnknownWhenDepNotInEphemeralData() {
        ProjectRecord main = registerRecord("mainPlugin", List.of("externalPlugin"));
        List<ProjectRecord> depsToFetch = new ArrayList<>();
        List<String> unknownDeps = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        resolved.add("mainplugin");

        service.resolve(List.of(main), resolved, depsToFetch, unknownDeps);

        assertTrue(depsToFetch.isEmpty());
        assertEquals(List.of("externalPlugin"), unknownDeps);
    }

    @Test
    void resolve_skipsAlreadyInstalledDependency() throws IOException {
        installJar("depPlugin");
        ProjectRecord dep = registerRecord("depPlugin", List.of());
        ProjectRecord main = registerRecord("mainPlugin", List.of("depPlugin"));
        List<ProjectRecord> depsToFetch = new ArrayList<>();
        List<String> unknownDeps = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        resolved.add("mainplugin");

        service.resolve(List.of(main), resolved, depsToFetch, unknownDeps);

        assertTrue(depsToFetch.isEmpty());
        assertTrue(unknownDeps.isEmpty());
    }

    @Test
    void resolve_skipsDepAlreadyInResolvedSet() {
        ProjectRecord dep = registerRecord("depPlugin", List.of());
        ProjectRecord main = registerRecord("mainPlugin", List.of("depPlugin"));
        List<ProjectRecord> depsToFetch = new ArrayList<>();
        List<String> unknownDeps = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        resolved.add("mainplugin");
        resolved.add("depplugin"); // already in batch

        service.resolve(List.of(main), resolved, depsToFetch, unknownDeps);

        assertTrue(depsToFetch.isEmpty());
        assertTrue(unknownDeps.isEmpty());
    }

    @Test
    void resolve_resolvesTransitiveDependencies() {
        ProjectRecord c = registerRecord("pluginC", List.of());
        ProjectRecord b = registerRecord("pluginB", List.of("pluginC"));
        ProjectRecord a = registerRecord("pluginA", List.of("pluginB"));
        List<ProjectRecord> depsToFetch = new ArrayList<>();
        List<String> unknownDeps = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        resolved.add("plugina");

        service.resolve(List.of(a), resolved, depsToFetch, unknownDeps);

        assertEquals(2, depsToFetch.size());
        assertTrue(depsToFetch.stream().anyMatch(r -> r.getName().equals("pluginB")));
        assertTrue(depsToFetch.stream().anyMatch(r -> r.getName().equals("pluginC")));
        assertTrue(unknownDeps.isEmpty());
    }

    @Test
    void resolve_handlesCircularDependenciesWithoutInfiniteLoop() {
        // A depends on B, B depends on A
        ProjectRecord a = registerRecord("pluginA", List.of("pluginB"));
        ProjectRecord b = registerRecord("pluginB", List.of("pluginA"));
        List<ProjectRecord> depsToFetch = new ArrayList<>();
        List<String> unknownDeps = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        resolved.add("plugina");

        // Should terminate and add pluginB exactly once (pluginA is already in resolved)
        service.resolve(List.of(a), resolved, depsToFetch, unknownDeps);

        assertEquals(1, depsToFetch.size());
        assertEquals("pluginB", depsToFetch.get(0).getName());
        assertTrue(unknownDeps.isEmpty());
    }
}
