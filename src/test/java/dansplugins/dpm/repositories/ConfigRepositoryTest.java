package dansplugins.dpm.repositories;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryTest {

    // -------------------------------------------------------------------------
    // saveMissingConfigDefaultsIfNotPresent()
    // -------------------------------------------------------------------------

    @Test
    void saveMissingConfigDefaultsIfNotPresent_seedsAllDefaultsOnEmptyConfig() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigRepository repository = repository(config, "v1.0", new ArrayList<>());

        repository.saveMissingConfigDefaultsIfNotPresent();

        assertEquals("v1.0", config.getString("version"));
        assertFalse(config.getBoolean("debugMode"));
        assertEquals("", config.getString("githubToken"));
        assertEquals("", config.getString("discordWebhook"));
        assertEquals("dev", config.getString("experimentalReleaseTag"));
    }

    @Test
    void saveMissingConfigDefaultsIfNotPresent_doesNotOverwriteExistingExperimentalReleaseTag() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("experimentalReleaseTag", "nightly");
        ConfigRepository repository = repository(config, "v1.0", new ArrayList<>());

        repository.saveMissingConfigDefaultsIfNotPresent();

        assertEquals("nightly", config.getString("experimentalReleaseTag"));
    }

    @Test
    void saveMissingConfigDefaultsIfNotPresent_overwritesVersionEvenWhenAlreadySet() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", "v0.9");
        ConfigRepository repository = repository(config, "v1.0", new ArrayList<>());

        repository.saveMissingConfigDefaultsIfNotPresent();

        assertEquals("v1.0", config.getString("version"));
    }

    @Test
    void saveMissingConfigDefaultsIfNotPresent_doesNotOverwriteExistingNonVersionOptions() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("debugMode", true);
        config.set("githubToken", "existing-token");
        ConfigRepository repository = repository(config, "v1.0", new ArrayList<>());

        repository.saveMissingConfigDefaultsIfNotPresent();

        assertTrue(config.getBoolean("debugMode"));
        assertEquals("existing-token", config.getString("githubToken"));
    }

    @Test
    void saveMissingConfigDefaultsIfNotPresent_invokesSaveAction() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> saveCalls = new ArrayList<>();
        ConfigRepository repository = repository(config, "v1.0", saveCalls);

        repository.saveMissingConfigDefaultsIfNotPresent();

        assertEquals(1, saveCalls.size(), "saveAction must be invoked exactly once");
    }

    // -------------------------------------------------------------------------
    // getConfig() / isSet() / getBoolean() / getString()
    // -------------------------------------------------------------------------

    @Test
    void getConfig_reflectsLiveConfigSupplier() {
        YamlConfiguration first = new YamlConfiguration();
        first.set("debugMode", true);
        YamlConfiguration second = new YamlConfiguration();
        second.set("debugMode", false);

        List<YamlConfiguration> configs = List.of(first, second);
        int[] callCount = {0};
        ConfigRepository repository = new ConfigRepository(
                () -> configs.get(Math.min(callCount[0]++, configs.size() - 1)),
                () -> "v1.0",
                () -> {});

        assertTrue(repository.getBoolean("debugMode"), "first supplier call must return the first config");
        assertFalse(repository.getBoolean("debugMode"), "second supplier call must return the second config");
    }

    @Test
    void isSet_returnsFalseForMissingOption() {
        ConfigRepository repository = repository(new YamlConfiguration(), "v1.0", new ArrayList<>());
        assertFalse(repository.isSet("githubToken"));
    }

    @Test
    void isSet_returnsTrueForPresentOption() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "abc");
        ConfigRepository repository = repository(config, "v1.0", new ArrayList<>());
        assertTrue(repository.isSet("githubToken"));
    }

    @Test
    void getString_returnsNullForMissingOption() {
        ConfigRepository repository = repository(new YamlConfiguration(), "v1.0", new ArrayList<>());
        assertNull(repository.getString("githubToken"));
    }

    // -------------------------------------------------------------------------
    // getStringOrDefault()
    // -------------------------------------------------------------------------

    @Test
    void getStringOrDefault_returnsStoredValueWhenPresent() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("githubToken", "abc123");
        ConfigRepository repository = repository(config, "v1.0", new ArrayList<>());

        assertEquals("abc123", repository.getStringOrDefault("githubToken", "fallback"));
    }

    @Test
    void getStringOrDefault_returnsDefaultWhenMissing() {
        ConfigRepository repository = repository(new YamlConfiguration(), "v1.0", new ArrayList<>());
        assertEquals("fallback", repository.getStringOrDefault("githubToken", "fallback"));
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------

    @Test
    void save_invokesSaveAction() {
        List<String> saveCalls = new ArrayList<>();
        ConfigRepository repository = repository(new YamlConfiguration(), "v1.0", saveCalls);

        repository.save();

        assertEquals(1, saveCalls.size());
    }

    // -------------------------------------------------------------------------
    // isUsageReportingEnabled() / getUsageReportingEndpoint() / getUsageReportingKey()
    // -------------------------------------------------------------------------

    @Test
    void usageReporting_fallsThroughToBundledDefaultsWhenOnDiskConfigPredatesTheBlock() {
        // A config.yml written by a version before usage reporting existed is
        // never rewritten, so the values must come from the jar's config.yml,
        // which Bukkit registers as the defaults of the on-disk file. This is
        // what the two-argument getters would break: they return their explicit
        // fallback instead of the bundled value.
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.set("version", "v0.6.0");
        onDisk.set("debugMode", false);
        onDisk.setDefaults(bundledConfig());
        ConfigRepository repository = repository(onDisk, "v1.0", new ArrayList<>());

        assertTrue(repository.isUsageReportingEnabled());
        assertEquals("https://trace.danielstephenson.dev", repository.getUsageReportingEndpoint());
        assertEquals(bundledConfig().getString("usage-reporting.key"), repository.getUsageReportingKey());
        assertFalse(repository.getUsageReportingKey().isEmpty(), "the bundled key must be read, not an empty fallback");
    }

    @Test
    void usageReporting_onDiskValuesTakePrecedenceOverBundledDefaults() {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.set("usage-reporting.enabled", false);
        onDisk.set("usage-reporting.endpoint", "http://localhost:1");
        onDisk.set("usage-reporting.key", "custom-key");
        onDisk.setDefaults(bundledConfig());
        ConfigRepository repository = repository(onDisk, "v1.0", new ArrayList<>());

        assertFalse(repository.isUsageReportingEnabled());
        assertEquals("http://localhost:1", repository.getUsageReportingEndpoint());
        assertEquals("custom-key", repository.getUsageReportingKey());
    }

    @Test
    void usageReporting_isOffWithSafeFallbacksWhenNeitherFileNorBundleHasTheBlock() {
        ConfigRepository repository = repository(new YamlConfiguration(), "v1.0", new ArrayList<>());

        assertFalse(repository.isUsageReportingEnabled());
        assertEquals("https://trace.danielstephenson.dev", repository.getUsageReportingEndpoint());
        assertEquals("", repository.getUsageReportingKey(), "no key anywhere must read as empty, which the client treats as off");
    }

    @Test
    void saveMissingConfigDefaultsIfNotPresent_copiesBundledUsageReportingBlockIntoTheSavedFile() {
        // Fresh installs and version upgrades go through this method, which
        // turns copyDefaults on before saving, so the block lands in the file.
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(bundledConfig());
        ConfigRepository repository = repository(config, "v1.0", new ArrayList<>());

        repository.saveMissingConfigDefaultsIfNotPresent();
        String saved = config.saveToString();

        assertTrue(saved.contains("usage-reporting:"), saved);
        assertTrue(saved.contains("enabled: true"), saved);
        assertTrue(saved.contains("endpoint: https://trace.danielstephenson.dev"), saved);
        assertTrue(saved.contains("key: " + bundledConfig().getString("usage-reporting.key")), saved);
        assertTrue(saved.contains("version: v1.0"), saved);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private ConfigRepository repository(YamlConfiguration config, String version, List<String> saveCalls) {
        return new ConfigRepository(() -> config, () -> version, () -> saveCalls.add("saved"));
    }

    /** The config.yml that ships inside the jar, loaded the way Bukkit loads it as defaults. */
    private YamlConfiguration bundledConfig() {
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(in, "src/main/resources/config.yml must be on the classpath");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
