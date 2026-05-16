package dansplugins.dpm.services;

import dansplugins.dpm.DansPluginManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigService {
    private final DansPluginManager dansPluginManager;

    private boolean altered = false;

    public ConfigService(DansPluginManager dansPluginManager) {
        this.dansPluginManager = dansPluginManager;
    }

    public void saveMissingConfigDefaultsIfNotPresent() {
        if (!getConfig().isString("version")) {
            getConfig().addDefault("version", dansPluginManager.getVersion());
        } else {
            getConfig().set("version", dansPluginManager.getVersion());
        }
        if (!isSet("debugMode")) {
            getConfig().set("debugMode", false);
        }
        if (!isSet("githubToken")) {
            getConfig().set("githubToken", "");
        }
        getConfig().options().copyDefaults(true);
        dansPluginManager.saveConfig();
    }

    public void setConfigOption(String option, String value, CommandSender sender) {
        if (!getConfig().isSet(option)) {
            sender.sendMessage(ChatColor.RED + "That config option wasn't found.");
            return;
        }
        if (option.equalsIgnoreCase("version")) {
            sender.sendMessage(ChatColor.RED + "Cannot set version.");
            return;
        }
        if (option.equalsIgnoreCase("debugMode")) {
            getConfig().set(option, Boolean.parseBoolean(value));
        } else {
            getConfig().set(option, value);
        }
        dansPluginManager.saveConfig();
        altered = true;
        sender.sendMessage(ChatColor.GREEN + "Config option updated.");
    }

    public void sendConfigList(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== Config ===");
        sender.sendMessage(ChatColor.AQUA + "version: " + getConfig().getString("version"));
        sender.sendMessage(ChatColor.AQUA + "debugMode: " + getConfig().getBoolean("debugMode"));
        String token = getConfig().getString("githubToken");
        String tokenDisplay = (token != null && !token.isEmpty()) ? "(set)" : "(not set)";
        sender.sendMessage(ChatColor.AQUA + "githubToken: " + tokenDisplay);
    }

    public boolean hasBeenAltered() {
        return altered;
    }

    public FileConfiguration getConfig() {
        return dansPluginManager.getConfig();
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
