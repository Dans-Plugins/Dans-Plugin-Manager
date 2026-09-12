package dansplugins.dpm.repositories;

import org.bukkit.configuration.ConfigurationSection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.Supplier;

public class ConfigRepository {
    private static final String USAGE_REPORTING_ENABLED_KEY = "usage-reporting.enabled";
    private static final String USAGE_REPORTING_ENDPOINT_KEY = "usage-reporting.endpoint";
    private static final String USAGE_REPORTING_KEY_KEY = "usage-reporting.key";
    private static final String USAGE_REPORTING_TAGS_KEY = "usage-reporting.tags";
    private static final String DEFAULT_USAGE_REPORTING_ENDPOINT = "https://trace.danielstephenson.dev";

    private final Supplier<FileConfiguration> configSupplier;
    private final Supplier<String> pluginVersionSupplier;
    private final Runnable saveAction;

    public ConfigRepository(Supplier<FileConfiguration> configSupplier, Supplier<String> pluginVersionSupplier, Runnable saveAction) {
        this.configSupplier = configSupplier;
        this.pluginVersionSupplier = pluginVersionSupplier;
        this.saveAction = saveAction;
    }

    public void saveMissingConfigDefaultsIfNotPresent() {
        FileConfiguration config = getConfig();
        if (!config.isString("version")) {
            config.addDefault("version", pluginVersionSupplier.get());
        } else {
            config.set("version", pluginVersionSupplier.get());
        }
        if (!isSet("debugMode")) {
            config.set("debugMode", false);
        }
        if (!isSet("githubToken")) {
            config.set("githubToken", "");
        }
        if (!isSet("discordWebhook")) {
            config.set("discordWebhook", "");
        }
        if (!isSet("experimentalReleaseTag")) {
            config.set("experimentalReleaseTag", GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG);
        }
        config.options().copyDefaults(true);
        saveAction.run();
    }

    public FileConfiguration getConfig() {
        return configSupplier.get();
    }

    public void save() {
        saveAction.run();
    }

    public boolean isSet(String option) {
        return getConfig().isSet(option);
    }

    public boolean getBoolean(String option) {
        return getConfig().getBoolean(option);
    }

    public String getString(String option) {
        return getConfig().getString(option);
    }

    public String getStringOrDefault(String option, String defaultValue) {
        String toReturn = getString(option);
        return toReturn != null ? toReturn : defaultValue;
    }

    // The one-argument getters, deliberately. The usage-reporting block is the
    // only part of config.yml that ships inside the jar, and nothing rewrites a
    // config.yml that already exists on disk, so a server upgraded from a
    // version before usage reporting has no usage-reporting block in its file.
    // Bukkit registers the jar's config.yml as the defaults for that file, and
    // the one-argument getters fall through to them -- but the two-argument
    // getters return their explicit fallback instead, which for the key would
    // be "" and would turn reporting off on every existing installation.
    // Verified against YamlConfiguration, not assumed.

    public boolean isUsageReportingEnabled() {
        return getBoolean(USAGE_REPORTING_ENABLED_KEY);
    }

    public String getUsageReportingEndpoint() {
        String endpoint = getString(USAGE_REPORTING_ENDPOINT_KEY);
        return endpoint != null ? endpoint : DEFAULT_USAGE_REPORTING_ENDPOINT;
    }

    /** Empty when no key is configured or bundled, which the client treats as "off". */
    public String getUsageReportingKey() {
        String key = getString(USAGE_REPORTING_KEY_KEY);
        return key != null ? key : "";
    }

    /**
     * Static tags attached to every usage event this installation reports, from
     * the optional {@code usage-reporting.tags} map. Empty by default. The
     * integration-test server sets {@code ci: "true"} here so its events can be
     * told apart from real installations.
     */
    public Map<String, String> getUsageReportingTags() {
        ConfigurationSection section = getConfig().getConfigurationSection(USAGE_REPORTING_TAGS_KEY);
        Map<String, String> tags = new LinkedHashMap<>();
        if (section == null) {
            return tags;
        }
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                tags.put(key, value);
            }
        }
        return tags;
    }
}
