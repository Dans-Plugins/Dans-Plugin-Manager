package dansplugins.dpm.controllers;

import dansplugins.dpm.repositories.ConfigRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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

    // Captures what the controller logs, so the empty-token warning can be asserted on directly.
    private static final class RecordingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override public void publish(LogRecord record) { records.add(record); }
        @Override public void flush() { }
        @Override public void close() { }
    }

    private static ConfigRepository configRepository(YamlConfiguration config) {
        return new ConfigRepository(() -> config, () -> "v1.0", () -> { });
    }

    // Each test gets its own named logger so a handler attached here never sees another test's records.
    private static Logger logger(String testName, RecordingHandler handler) {
        Logger logger = Logger.getLogger("ReloadControllerTest." + testName);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
        return logger;
    }

    private static ReloadController controller(Runnable configReloader, YamlConfiguration config, List<String> events) {
        return new ReloadController(configReloader, configRepository(config), new RecordingReleaseRepository(events),
                Logger.getLogger("ReloadControllerTest"));
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
        ReloadController controller = controller(() -> events.add("reloadConfig"), config, events);

        controller.applySettings();

        assertEquals(List.of("token=ghp_secret", "tag=nightly"), events);
    }

    @Test
    void applySettings_fallsBackToDefaultsWhenConfigKeysAreAbsent() {
        List<String> events = new ArrayList<>();
        ReloadController controller = controller(() -> events.add("reloadConfig"), new YamlConfiguration(), events);

        controller.applySettings();

        assertEquals(List.of("token=", "tag=" + GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG), events);
    }

    @Test
    void applySettings_doesNotClearTheReleaseCache() {
        List<String> events = new ArrayList<>();
        ReloadController controller = controller(() -> events.add("reloadConfig"), new YamlConfiguration(), events);

        controller.applySettings();

        assertFalse(events.contains("clearCache"));
        assertFalse(events.contains("reloadConfig"));
    }

    @Test
    void applySettings_warnsWhenGithubTokenIsEmpty() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "");
        RecordingHandler log = new RecordingHandler();
        ReloadController controller = new ReloadController(() -> { }, configRepository(config),
                new RecordingReleaseRepository(new ArrayList<>()), logger("emptyToken", log));

        controller.applySettings();

        assertEquals(1, log.records.size());
        LogRecord warning = log.records.get(0);
        assertEquals(Level.WARNING, warning.getLevel());
        assertTrue(warning.getMessage().contains("githubToken is not set"), warning.getMessage());
        assertTrue(warning.getMessage().contains("60 per hour"), warning.getMessage());
        assertTrue(warning.getMessage().contains("/dpm reload"), warning.getMessage());
    }

    @Test
    void applySettings_warnsWhenGithubTokenIsAbsentFromConfig() {
        RecordingHandler log = new RecordingHandler();
        ReloadController controller = new ReloadController(() -> { }, configRepository(new YamlConfiguration()),
                new RecordingReleaseRepository(new ArrayList<>()), logger("absentToken", log));

        controller.applySettings();

        assertEquals(1, log.records.size());
        assertEquals(Level.WARNING, log.records.get(0).getLevel());
    }

    @Test
    void applySettings_treatsWhitespaceOnlyTokenAsEmpty() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "   ");
        RecordingHandler log = new RecordingHandler();
        ReloadController controller = new ReloadController(() -> { }, configRepository(config),
                new RecordingReleaseRepository(new ArrayList<>()), logger("blankToken", log));

        controller.applySettings();

        assertEquals(1, log.records.size());
        assertTrue(log.records.get(0).getMessage().contains("githubToken is not set"));
    }

    @Test
    void applySettings_doesNotWarnWhenGithubTokenIsSet() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "ghp_secret");
        RecordingHandler log = new RecordingHandler();
        ReloadController controller = new ReloadController(() -> { }, configRepository(config),
                new RecordingReleaseRepository(new ArrayList<>()), logger("tokenSet", log));

        controller.applySettings();

        assertTrue(log.records.isEmpty(), () -> "unexpected log: " + log.records.get(0).getMessage());
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
        ReloadController controller = controller(configReloader, config, events);

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
        ReloadController controller = controller(() -> events.add("reloadConfig"), config, events);

        controller.reload();

        assertEquals(List.of("reloadConfig", "token=ghp_secret", "tag=nightly", "clearCache"), events);
    }

    @Test
    void reload_appliesDefaultsWhenConfigKeysAreAbsent() {
        List<String> events = new ArrayList<>();
        ReloadController controller = controller(() -> events.add("reloadConfig"), new YamlConfiguration(), events);

        controller.reload();

        assertEquals(List.of("reloadConfig", "token=", "tag=" + GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG,
                "clearCache"), events);
    }

    @Test
    void reload_stopsWarningOnceATokenHasBeenAdded() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "");
        RecordingHandler log = new RecordingHandler();
        // Stands in for the operator adding a token to config.yml between startup and /dpm reload.
        Runnable configReloader = () -> config.set("githubToken", "ghp_secret");
        ReloadController controller = new ReloadController(configReloader, configRepository(config),
                new RecordingReleaseRepository(new ArrayList<>()), logger("reloadWithToken", log));

        controller.applySettings();
        assertEquals(1, log.records.size(), "startup with an empty token should warn once");

        controller.reload();
        assertEquals(1, log.records.size(), "reload with a token present should not warn again");
    }
}
