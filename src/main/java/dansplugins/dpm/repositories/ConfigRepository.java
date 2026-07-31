package dansplugins.dpm.repositories;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.Supplier;

public class ConfigRepository {
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
}
