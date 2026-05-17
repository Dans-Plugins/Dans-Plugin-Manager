package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.DownloadService;
import dansplugins.dpm.services.PluginFolderService;
import dansplugins.dpm.services.VersionStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final DownloadService downloadService;
    private final PluginFolderService pluginFolderService;
    private final VersionStore versionStore;
    private final Plugin plugin;

    public UpdateCommand(EphemeralData ephemeralData, DownloadService downloadService,
                         PluginFolderService pluginFolderService, VersionStore versionStore,
                         Plugin plugin) {
        super(new ArrayList<>(List.of("update")), new ArrayList<>(List.of("dpm.update")));
        this.ephemeralData = ephemeralData;
        this.downloadService = downloadService;
        this.pluginFolderService = pluginFolderService;
        this.versionStore = versionStore;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender) {
        List<ProjectRecord> installed = getInstalledPlugins();
        if (installed.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No managed plugins are currently installed.");
            return true;
        }
        sender.sendMessage(ChatColor.AQUA + "Checking " + installed.size() + " installed plugin(s) for updates...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runUpdates(sender, installed));
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return execute(sender);
        return executeSelective(sender, args);
    }

    private boolean executeSelective(CommandSender sender, String[] names) {
        List<ProjectRecord> candidates = new ArrayList<>();
        for (String name : names) {
            ProjectRecord record = ephemeralData.getProjectRecord(name);
            if (record == null) {
                sender.sendMessage(ChatColor.RED + "Plugin not found: " + name + ". Use /dpm search <keyword> to find the right name.");
            } else {
                candidates.add(record);
            }
        }
        Set<String> installedNames = pluginFolderService.filterInstalled(candidates)
                .stream().map(ProjectRecord::getName).collect(Collectors.toSet());
        List<ProjectRecord> toUpdate = new ArrayList<>();
        for (ProjectRecord r : candidates) {
            if (installedNames.contains(r.getName())) {
                toUpdate.add(r);
            } else {
                sender.sendMessage(ChatColor.YELLOW + r.getName() + " is not installed — use /dpm get " + r.getName() + " first.");
            }
        }
        if (toUpdate.isEmpty()) return false;
        sender.sendMessage(ChatColor.AQUA + "Checking " + toUpdate.size() + " plugin(s) for updates...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runUpdates(sender, toUpdate));
        return true;
    }

    public List<String> getInstalledPluginNames() {
        return pluginFolderService.filterInstalled(ephemeralData.getAllProjectRecords())
                .stream().map(ProjectRecord::getName).collect(Collectors.toList());
    }

    private List<ProjectRecord> getInstalledPlugins() {
        return pluginFolderService.filterInstalled(ephemeralData.getAllProjectRecords());
    }

    private void runUpdates(CommandSender sender, List<ProjectRecord> records) {
        int updated = 0;
        int upToDate = 0;
        int skipped = 0;
        int failed = 0;

        for (ProjectRecord record : records) {
            String oldTag = versionStore.getStoredTag(record.getName());
            int result = downloadService.downloadLatest(record, true);
            final String msg;
            if (result == DownloadService.ALREADY_UP_TO_DATE) {
                upToDate++;
                msg = ChatColor.GREEN + record.getName() + (oldTag != null ? " " + oldTag : "") + " already up to date.";
            } else if (result == DownloadService.NO_RELEASE) {
                skipped++;
                msg = ChatColor.YELLOW + record.getName() + " has no published release yet.";
            } else if (result > 0) {
                updated++;
                String newTag = versionStore.getStoredTag(record.getName());
                String versionDiff = oldTag != null && newTag != null
                        ? " " + oldTag + " → " + newTag
                        : newTag != null ? " " + newTag : "";
                msg = ChatColor.GREEN + "Updated " + record.getName() + versionDiff + ".";
            } else {
                failed++;
                msg = ChatColor.RED + "Failed to update " + record.getName() + ".";
            }
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
        }

        final int finalUpdated = updated;
        final int finalUpToDate = upToDate;
        final int finalSkipped = skipped;
        final int finalFailed = failed;
        Bukkit.getScheduler().runTask(plugin, () -> {
            StringBuilder summary = new StringBuilder();
            summary.append(ChatColor.AQUA).append("Update complete: ")
                   .append(ChatColor.GREEN).append(finalUpdated).append(" updated")
                   .append(ChatColor.AQUA).append(", ").append(finalUpToDate).append(" already up to date");
            if (finalSkipped > 0) {
                summary.append(ChatColor.AQUA).append(", ").append(finalSkipped).append(" skipped (no release)");
            }
            if (finalFailed > 0) {
                summary.append(ChatColor.RED).append(", ").append(finalFailed).append(" failed");
            }
            summary.append(ChatColor.AQUA).append(".");
            sender.sendMessage(summary.toString());
            if (finalUpdated > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Restart the server to load updated plugins.");
            }
        });
    }
}
