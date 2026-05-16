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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final DownloadService downloadService;
    private final PluginFolderService pluginFolderService;
    private final VersionStore versionStore;
    private final Plugin plugin;

    public GetCommand(EphemeralData ephemeralData, DownloadService downloadService,
                      PluginFolderService pluginFolderService, VersionStore versionStore, Plugin plugin) {
        super(new ArrayList<>(List.of("get")), new ArrayList<>(List.of("dpm.get")));
        this.ephemeralData = ephemeralData;
        this.downloadService = downloadService;
        this.pluginFolderService = pluginFolderService;
        this.versionStore = versionStore;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /dpm get <plugin-name> [plugin-name ...]");
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return executeSingle(sender, args[0]);
        }
        return executeBatch(sender, args);
    }

    private boolean executeSingle(CommandSender sender, String name) {
        ProjectRecord record = ephemeralData.getProjectRecord(name);
        if (record == null) {
            sender.sendMessage(ChatColor.RED + "Plugin not found: " + name);
            return false;
        }
        warnMissingDependencies(sender, record, Collections.emptySet());
        sender.sendMessage(ChatColor.AQUA + "Fetching " + record.getName() + "...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int result = downloadService.downloadLatest(record);
            Bukkit.getScheduler().runTask(plugin, () -> reportSingleResult(sender, record, result));
        });
        return true;
    }

    private boolean executeBatch(CommandSender sender, String[] args) {
        // First pass: resolve all names so the full batch set is known before warnings fire.
        List<ProjectRecord> records = new ArrayList<>();
        int notFound = 0;
        for (String name : args) {
            ProjectRecord record = ephemeralData.getProjectRecord(name);
            if (record == null) {
                sender.sendMessage(ChatColor.RED + "Plugin not found: " + name + " — skipping.");
                notFound++;
            } else {
                records.add(record);
            }
        }

        // Warn about missing deps, suppressing warnings for deps already in this batch.
        Set<String> batchNames = new HashSet<>();
        for (ProjectRecord r : records) batchNames.add(r.getName());
        for (ProjectRecord record : records) {
            warnMissingDependencies(sender, record, batchNames);
        }

        if (records.isEmpty()) return false;
        sender.sendMessage(ChatColor.AQUA + "Fetching " + records.size() + " plugin(s)...");
        final int fn = notFound;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runBatch(sender, records, fn));
        return true;
    }

    private void runBatch(CommandSender sender, List<ProjectRecord> records, int notFound) {
        int downloaded = 0, upToDate = 0, skipped = 0, failed = 0;
        for (ProjectRecord record : records) {
            int result = downloadService.downloadLatest(record);
            final String msg;
            if (result == DownloadService.NO_RELEASE) {
                skipped++;
                msg = ChatColor.YELLOW + record.getName() + " has no published release yet.";
            } else if (result == DownloadService.ALREADY_UP_TO_DATE) {
                upToDate++;
                String tag = versionStore.getStoredTag(record.getName());
                msg = ChatColor.GREEN + record.getName() + (tag != null ? " " + tag : "") + " already up to date.";
            } else if (result <= 0) {
                failed++;
                msg = ChatColor.RED + "Failed to download " + record.getName() + ".";
            } else {
                downloaded++;
                String tag = versionStore.getStoredTag(record.getName());
                String version = tag != null ? " " + tag : "";
                msg = ChatColor.GREEN + "Downloaded " + record.getName() + version + " (" + (result / 1024) + " KB).";
            }
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
        }
        final int fd = downloaded, fu = upToDate, fs = skipped, ff = failed;
        Bukkit.getScheduler().runTask(plugin, () -> {
            StringBuilder summary = new StringBuilder();
            summary.append(ChatColor.AQUA).append("Done: ")
                   .append(ChatColor.GREEN).append(fd).append(" downloaded")
                   .append(ChatColor.AQUA).append(", ").append(fu).append(" already up to date");
            if (fs > 0) summary.append(ChatColor.AQUA).append(", ").append(fs).append(" skipped (no release)");
            if (ff > 0) summary.append(ChatColor.RED).append(", ").append(ff).append(" failed");
            if (notFound > 0) summary.append(ChatColor.RED).append(", ").append(notFound).append(" not found");
            summary.append(ChatColor.AQUA).append(".");
            sender.sendMessage(summary.toString());
            if (fd > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Restart the server to enable downloaded plugins.");
            }
        });
    }

    private void reportSingleResult(CommandSender sender, ProjectRecord record, int result) {
        if (result == DownloadService.NO_RELEASE) {
            sender.sendMessage(ChatColor.YELLOW + record.getName() + " has no published release yet. Try again later.");
        } else if (result == DownloadService.ALREADY_UP_TO_DATE) {
            String tag = versionStore.getStoredTag(record.getName());
            String version = tag != null ? " (" + tag + ")" : "";
            sender.sendMessage(ChatColor.GREEN + record.getName() + " is already up to date" + version + ".");
        } else if (result <= 0) {
            sender.sendMessage(ChatColor.RED + "Something went wrong downloading " + record.getName() + ".");
        } else {
            String tag = versionStore.getStoredTag(record.getName());
            String version = tag != null ? " " + tag : "";
            sender.sendMessage(ChatColor.GREEN + "Downloaded" + version + " (" + (result / 1024) + " KB). Restart the server to enable " + record.getName() + ".");
        }
    }

    private void warnMissingDependencies(CommandSender sender, ProjectRecord record, Set<String> batchNames) {
        for (String dep : record.getHardDependencies()) {
            if (batchNames.contains(dep)) continue;
            ProjectRecord depRecord = ephemeralData.getProjectRecord(dep);
            if (depRecord == null || !pluginFolderService.isInstalled(depRecord)) {
                sender.sendMessage(ChatColor.YELLOW + "Warning: " + record.getName()
                        + " requires " + dep + ", which is not installed. Run /dpm get " + dep + " first.");
            }
        }
    }
}
