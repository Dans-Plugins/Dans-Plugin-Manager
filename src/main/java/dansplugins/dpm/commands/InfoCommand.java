package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.InfoController;
import dansplugins.dpm.controllers.InfoController.PluginInfo;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class InfoCommand extends AbstractPluginCommand {
    private final InfoController infoController;
    private final Plugin plugin;

    public InfoCommand(InfoController infoController, Plugin plugin) {
        super(new ArrayList<>(List.of("info")), new ArrayList<>(List.of("dpm.info")));
        this.infoController = infoController;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /dpm info <plugin-name>");
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String name = args[0];
        ProjectRecord record = infoController.getRecord(name);
        if (record == null) {
            sender.sendMessage(ChatColor.RED + "Plugin not found: " + name + ". Use /dpm search <keyword> to find the right name.");
            return false;
        }
        sender.sendMessage(ChatColor.AQUA + "Fetching release info for " + record.getName() + "...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PluginInfo info = infoController.getInfo(record);
            Bukkit.getScheduler().runTask(plugin, () -> showInfo(sender, info));
        });
        return true;
    }

    private void showInfo(CommandSender sender, PluginInfo info) {
        ProjectRecord record = info.getRecord();
        ReleaseInfo release = info.getRelease();
        ReleaseChannel channel = info.getChannel();
        sender.sendMessage(ChatColor.AQUA + "=== " + record.getName() + " ===");

        if (record.getDescription() != null) {
            sender.sendMessage(ChatColor.WHITE + record.getDescription());
        }

        sender.sendMessage(ChatColor.WHITE + "Owner: " + ChatColor.AQUA + record.getOwner());
        sender.sendMessage(ChatColor.WHITE + "Repository: " + ChatColor.AQUA + record.getRepo());
        sender.sendMessage(ChatColor.WHITE + "Channel: "
                + (channel == ReleaseChannel.EXPERIMENTAL ? ChatColor.YELLOW : ChatColor.AQUA)
                + channel.getDisplayName());

        String releaseLabel = channel == ReleaseChannel.EXPERIMENTAL ? "Latest experimental build" : "Latest release";
        if (release == ReleaseInfo.NO_RELEASE) {
            sender.sendMessage(ChatColor.YELLOW + releaseLabel + ": None published yet");
        } else if (release == null) {
            sender.sendMessage(ChatColor.RED + releaseLabel + ": (could not fetch — check console for details)");
        } else {
            sender.sendMessage(ChatColor.WHITE + releaseLabel + ": " + ChatColor.GREEN + release.getTagName());
            if (release.getPublishedAt() != null) {
                sender.sendMessage(ChatColor.WHITE + "Published: " + ChatColor.AQUA + formatDate(release.getPublishedAt()));
            }
        }

        if (info.isInstalled()) {
            String version = info.getStoredTag() != null ? info.getStoredTag() : "(version unknown)";
            sender.sendMessage(ChatColor.WHITE + "Installed: " + ChatColor.GREEN + "Yes (" + version + ")");
            if (info.hasPublishedRelease()) {
                if (info.isUpToDate()) {
                    sender.sendMessage(ChatColor.WHITE + "Status: " + ChatColor.GREEN + "Up to date");
                } else {
                    sender.sendMessage(ChatColor.WHITE + "Status: " + ChatColor.YELLOW + "Update available");
                }
            }
        } else {
            sender.sendMessage(ChatColor.WHITE + "Installed: " + ChatColor.GRAY + "No");
        }

        showDependencies(sender, record.getHardDependencies(), "Requires", info);
        showDependencies(sender, record.getSoftDependencies(), "Integrates with", info);
    }

    private void showDependencies(CommandSender sender, List<String> deps, String label, PluginInfo info) {
        if (deps.isEmpty()) return;
        for (String dep : deps) {
            String status = info.isDependencyInstalled(dep)
                    ? ChatColor.GREEN + dep + " (installed)"
                    : ChatColor.RED + dep + " (not installed)";
            sender.sendMessage(ChatColor.WHITE + label + ": " + status);
        }
    }

    /** Trims the time component from an ISO 8601 timestamp, returning just the date. */
    private String formatDate(String iso8601) {
        int tIndex = iso8601.indexOf('T');
        return tIndex > 0 ? iso8601.substring(0, tIndex) : iso8601;
    }
}
