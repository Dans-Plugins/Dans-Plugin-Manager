package dansplugins.dpm.controllers;

import dansplugins.dpm.repositories.ConfigRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ConfigController {
    private final ConfigRepository configRepository;

    private boolean altered = false;

    public ConfigController(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public void setConfigOption(String option, String value, CommandSender sender) {
        if (!configRepository.isSet(option)) {
            sender.sendMessage(ChatColor.RED + "That config option wasn't found.");
            return;
        }
        if (option.equalsIgnoreCase("version")) {
            sender.sendMessage(ChatColor.RED + "Cannot set version.");
            return;
        }
        if (option.equalsIgnoreCase("debugMode")) {
            configRepository.getConfig().set(option, Boolean.parseBoolean(value));
        } else {
            configRepository.getConfig().set(option, value);
        }
        configRepository.save();
        altered = true;
        sender.sendMessage(ChatColor.GREEN + "Config option updated.");
    }

    public void sendConfigList(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== Config ===");
        sender.sendMessage(ChatColor.AQUA + "version: " + configRepository.getString("version"));
        sender.sendMessage(ChatColor.AQUA + "debugMode: " + configRepository.getBoolean("debugMode"));
        String token = configRepository.getString("githubToken");
        String tokenDisplay = (token != null && !token.isEmpty()) ? "(set)" : "(not set)";
        sender.sendMessage(ChatColor.AQUA + "githubToken: " + tokenDisplay);
        String webhook = configRepository.getString("discordWebhook");
        String webhookDisplay = (webhook != null && !webhook.isEmpty()) ? "(set)" : "(not set)";
        sender.sendMessage(ChatColor.AQUA + "discordWebhook: " + webhookDisplay);
        sender.sendMessage(ChatColor.AQUA + "experimentalReleaseTag: "
                + configRepository.getStringOrDefault("experimentalReleaseTag", "dev"));
    }

    public boolean hasBeenAltered() {
        return altered;
    }
}
