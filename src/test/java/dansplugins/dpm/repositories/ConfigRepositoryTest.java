package dansplugins.dpm.repositories;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

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
    // helpers
    // -------------------------------------------------------------------------

    private ConfigRepository repository(YamlConfiguration config, String version, List<String> saveCalls) {
        return new ConfigRepository(() -> config, () -> version, () -> saveCalls.add("saved"));
    }
}
