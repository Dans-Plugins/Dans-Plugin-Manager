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
        return execute(sender);
    }

    private List<ProjectRecord> getInstalledPlugins() {
        List<ProjectRecord> installed = new ArrayList<>();
        for (ProjectRecord record : ephemeralData.getAllProjectRecords()) {
            if (pluginFolderService.isInstalled(record)) {
                installed.add(record);
            }
        }
        return installed;
    }

    private void runUpdates(CommandSender sender, List<ProjectRecord> records) {
        int updated = 0;
        int upToDate = 0;
        int failed = 0;

        for (ProjectRecord record : records) {
            int result = downloadService.downloadLatest(record);
            if (result == DownloadService.ALREADY_UP_TO_DATE) {
                upToDate++;
            } else if (result > 0) {
                updated++;
                String tag = versionStore.getStoredTag(record.getName());
                String version = tag != null ? " " + tag : "";
                final String msg = ChatColor.GREEN + "Updated " + record.getName() + version + ".";
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
            } else {
                failed++;
                final String msg = ChatColor.RED + "Failed to update " + record.getName() + ".";
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
            }
        }

        final int finalUpdated = updated;
        final int finalUpToDate = upToDate;
        final int finalFailed = failed;
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage(ChatColor.AQUA + "Update complete: "
                    + ChatColor.GREEN + finalUpdated + " updated"
                    + ChatColor.AQUA + ", " + finalUpToDate + " already up to date"
                    + (finalFailed > 0 ? ChatColor.RED + ", " + finalFailed + " failed" : "") + ".");
            if (finalUpdated > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Restart the server to load updated plugins.");
            }
        });
    }
}
