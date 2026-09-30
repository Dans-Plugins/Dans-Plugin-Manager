package dansplugins.dpm.factories;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRecordFactoryTest {

    private ProjectRecordRepository repository;
    private ProjectRecordFactory factory;

    @BeforeEach
    void setUp() {
        repository = new ProjectRecordRepository();
        factory = new ProjectRecordFactory(repository);
    }

    // -------------------------------------------------------------------------
    // createGitHubRecord()
    // -------------------------------------------------------------------------

    @Test
    void createGitHubRecord_addsRecordWithGivenCoordinates() {
        factory.createGitHubRecord("currencies", "Dans-Plugins", "Currencies");

        ProjectRecord record = repository.getProjectRecord("currencies");
        assertNotNull(record);
        assertEquals("Dans-Plugins", record.getOwner());
        assertEquals("Currencies", record.getRepo());
    }

    @Test
    void createGitHubRecord_leavesDescriptionAndDependenciesUnset() {
        factory.createGitHubRecord("currencies", "Dans-Plugins", "Currencies");

        ProjectRecord record = repository.getProjectRecord("currencies");
        assertNull(record.getDescription());
        assertTrue(record.getHardDependencies().isEmpty());
        assertTrue(record.getSoftDependencies().isEmpty());
    }

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------

    @Test
    void register_addsTheSameRecordInstance() {
        ProjectRecord record = ProjectRecord.builder("fiefs", "Dans-Plugins", "Fiefs")
                .hardDependencies(List.of("medievalfactions"))
                .build();

        factory.register(record);

        assertSame(record, repository.getProjectRecord("fiefs"));
    }

    @Test
    void register_doesNotDeduplicateRepeatedNames() {
        factory.register(ProjectRecord.forGitHub("fiefs", "Dans-Plugins", "Fiefs"));
        factory.register(ProjectRecord.forGitHub("fiefs", "Dans-Plugins", "Fiefs"));

        assertEquals(2, repository.getNumProjectRecords());
    }
}
