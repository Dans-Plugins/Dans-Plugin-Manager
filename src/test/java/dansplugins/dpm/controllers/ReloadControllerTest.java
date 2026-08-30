package dansplugins.dpm.controllers;

import dansplugins.dpm.repositories.ConfigRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReloadControllerTest {

    // Records what the controller pushed into the release repository, and in what order, so the
    // ordering between re-reading the config and dropping the cache can be asserted.
    private static final class RecordingReleaseRepository extends GitHubReleaseRepository {
        private final List<String> events;

        RecordingReleaseRepository(List<String> events) {
            super(null);
            this.events = events;
        }

        @Override
        public void setApiToken(String token) {
            events.add("token=" + token);
        }

        @Override
        public void setExperimentalTag(String tag) {
            events.add("tag=" + tag);
        }

        @Override
        public void clearCache() {
            events.add("clearCache");
        }
    }

    private static ConfigRepository configRepository(YamlConfiguration config) {
        return new ConfigRepository(() -> config, () -> "v1.0", () -> { });
    }

    // -------------------------------------------------------------------------
    // applySettings()
    // -------------------------------------------------------------------------

    @Test
    void applySettings_copiesTokenAndExperimentalTagFromConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "ghp_secret");
        config.set("experimentalReleaseTag", "nightly");
        List<String> events = new ArrayList<>();
        ReloadController controller = new ReloadController(
                () -> events.add("reloadConfig"), configRepository(config), new RecordingReleaseRepository(events));

        controller.applySettings();

        assertEquals(List.of("token=ghp_secret", "tag=nightly"), events);
    }

    @Test
    void applySettings_fallsBackToDefaultsWhenConfigKeysAreAbsent() {
        List<String> events = new ArrayList<>();
        ReloadController controller = new ReloadController(
                () -> events.add("reloadConfig"), configRepository(new YamlConfiguration()),
                new RecordingReleaseRepository(events));

        controller.applySettings();

        assertEquals(List.of("token=", "tag=" + GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG), events);
    }

    @Test
    void applySettings_doesNotClearTheReleaseCache() {
        List<String> events = new ArrayList<>();
        ReloadController controller = new ReloadController(
                () -> events.add("reloadConfig"), configRepository(new YamlConfiguration()),
                new RecordingReleaseRepository(events));

        controller.applySettings();

        assertFalse(events.contains("clearCache"));
        assertFalse(events.contains("reloadConfig"));
    }

    // -------------------------------------------------------------------------
    // reload()
    // -------------------------------------------------------------------------

    @Test
    void reload_rereadsConfigBeforeReadingTheSettingsFromIt() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "stale");
        List<String> events = new ArrayList<>();
        // Stands in for Bukkit re-reading config.yml from disk: the value the controller reads
        // afterwards must be the post-reload one, not the one present when reload() was called.
        Runnable configReloader = () -> {
            events.add("reloadConfig");
            config.set("githubToken", "fresh");
        };
        ReloadController controller = new ReloadController(
                configReloader, configRepository(config), new RecordingReleaseRepository(events));

        controller.reload();

        assertEquals("reloadConfig", events.get(0));
        assertTrue(events.contains("token=fresh"));
        assertFalse(events.contains("token=stale"));
    }

    @Test
    void reload_clearsTheReleaseCacheAfterApplyingTheNewSettings() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "ghp_secret");
        config.set("experimentalReleaseTag", "nightly");
        List<String> events = new ArrayList<>();
        ReloadController controller = new ReloadController(
                () -> events.add("reloadConfig"), configRepository(config), new RecordingReleaseRepository(events));

        controller.reload();

        assertEquals(List.of("reloadConfig", "token=ghp_secret", "tag=nightly", "clearCache"), events);
    }

    @Test
    void reload_appliesDefaultsWhenConfigKeysAreAbsent() {
        List<String> events = new ArrayList<>();
        ReloadController controller = new ReloadController(
                () -> events.add("reloadConfig"), configRepository(new YamlConfiguration()),
                new RecordingReleaseRepository(events));

        controller.reload();

        assertEquals(List.of("reloadConfig", "token=", "tag=" + GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG,
                "clearCache"), events);
    }
}
