package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.GetController;
import dansplugins.dpm.controllers.GetController.DependencyResolutionResult;
import dansplugins.dpm.controllers.GetController.PluginResult;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.objects.ProjectRecord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class GetCommand extends AbstractPluginCommand {
    private final ProjectRecordRepository projectRecordRepository;
    private final GetController getController;
    private final Plugin plugin;

    public GetCommand(ProjectRecordRepository projectRecordRepository, GetController getController, Plugin plugin) {
        super(new ArrayList<>(List.of("get")), new ArrayList<>(List.of("dpm.get")));
        this.projectRecordRepository = projectRecordRepository;
        this.getController = getController;
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
        ProjectRecord record = projectRecordRepository.getProjectRecord(name);
        if (record == null) {
            sender.sendMessage(ChatColor.RED + "Plugin not found: " + name + ". Use /dpm search <keyword> to find the right name.");
            return false;
        }

        DependencyResolutionResult resolution = getController.resolveDependencies(List.of(record));
        for (String dep : resolution.getUnknownDeps()) {
            sender.sendMessage(ChatColor.YELLOW + "Warning: " + record.getName()
                    + " requires " + dep + ", which is not installed and is not a managed DPC plugin.");
        }

        List<ProjectRecord> depsToFetch = resolution.getDepsToFetch();
        if (depsToFetch.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "Fetching " + record.getName() + "...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                PluginResult result = getController.download(record);
                Bukkit.getScheduler().runTask(plugin, () -> reportSingleResult(sender, result));
            });
        } else {
            for (ProjectRecord dep : depsToFetch) {
                sender.sendMessage(ChatColor.AQUA + "Info: Also downloading required dependency " + dep.getName() + ".");
            }
            List<ProjectRecord> allToFetch = new ArrayList<>(depsToFetch);
            allToFetch.add(record);
            sender.sendMessage(ChatColor.AQUA + "Fetching " + allToFetch.size() + " plugin(s)...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runBatch(sender, allToFetch, 0));
        }
        return true;
    }

    private boolean executeBatch(CommandSender sender, String[] args) {
        List<ProjectRecord> records = new ArrayList<>();
        int notFound = 0;
        for (String name : args) {
            ProjectRecord record = projectRecordRepository.getProjectRecord(name);
            if (record == null) {
                sender.sendMessage(ChatColor.RED + "Plugin not found: " + name + " — skipping. Use /dpm search <keyword> to find the right name.");
                notFound++;
            } else {
                records.add(record);
            }
        }

        DependencyResolutionResult resolution = getController.resolveDependencies(records);
        for (String dep : resolution.getUnknownDeps()) {
            sender.sendMessage(ChatColor.YELLOW + "Warning: required dependency '" + dep
                    + "' is not installed and is not a managed DPC plugin.");
        }

        List<ProjectRecord> depsToFetch = resolution.getDepsToFetch();
        List<ProjectRecord> allToFetch = new ArrayList<>(depsToFetch);
        for (ProjectRecord dep : depsToFetch) {
            sender.sendMessage(ChatColor.AQUA + "Info: Also downloading required dependency " + dep.getName() + ".");
        }
        allToFetch.addAll(records);

        if (allToFetch.isEmpty()) return false;
        sender.sendMessage(ChatColor.AQUA + "Fetching " + allToFetch.size() + " plugin(s)...");
        final int fn = notFound;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runBatch(sender, allToFetch, fn));
        return true;
    }

    private void runBatch(CommandSender sender, List<ProjectRecord> records, int notFound) {
        List<PluginResult> results = getController.runBatch(records);
        int downloaded = 0, upToDate = 0, skipped = 0, failed = 0;
        for (PluginResult result : results) {
            ProjectRecord record = result.getRecord();
            String msg;
            switch (result.getOutcome()) {
                case NO_RELEASE:
                    skipped++;
                    msg = ChatColor.YELLOW + record.getName() + " has no published release yet.";
                    break;
                case ALREADY_UP_TO_DATE:
                    upToDate++;
                    String tag = result.getStoredTag();
                    msg = ChatColor.GREEN + record.getName() + (tag != null ? " " + tag : "") + " already up to date.";
                    break;
                case NETWORK_ERROR:
                    failed++;
                    msg = ChatColor.RED + "Failed to download " + record.getName() + " (could not reach GitHub — check console for details).";
                    break;
                case FILE_ERROR:
                    failed++;
                    msg = ChatColor.RED + "Failed to download " + record.getName() + " (could not write to plugins folder — check server file permissions).";
                    break;
                case DOWNLOADED:
                    downloaded++;
                    String version = result.getStoredTag() != null ? " " + result.getStoredTag() : "";
                    msg = ChatColor.GREEN + "Downloaded " + record.getName() + version + " (" + (result.getDownloadedBytes() / 1024) + " KB).";
                    break;
                default:
                    failed++;
                    msg = ChatColor.RED + "Failed to download " + record.getName() + ".";
                    break;
            }
            final String fmsg = msg;
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(fmsg));
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

    private void reportSingleResult(CommandSender sender, PluginResult result) {
        ProjectRecord record = result.getRecord();
        switch (result.getOutcome()) {
            case NO_RELEASE:
                sender.sendMessage(ChatColor.YELLOW + record.getName() + " has no published release yet. Try again later.");
                break;
            case ALREADY_UP_TO_DATE:
                String tag = result.getStoredTag();
                String upToDateVersion = tag != null ? " (" + tag + ")" : "";
                sender.sendMessage(ChatColor.GREEN + record.getName() + " is already up to date" + upToDateVersion + ".");
                break;
            case NETWORK_ERROR:
                sender.sendMessage(ChatColor.RED + "Could not reach GitHub when downloading " + record.getName() + " — check console for details.");
                break;
            case FILE_ERROR:
                sender.sendMessage(ChatColor.RED + "Could not write " + record.getName() + " to the plugins folder — check server file permissions.");
                break;
            case DOWNLOADED:
                String downloadedVersion = result.getStoredTag() != null ? " " + result.getStoredTag() : "";
                sender.sendMessage(ChatColor.GREEN + "Downloaded" + downloadedVersion + " (" + (result.getDownloadedBytes() / 1024) + " KB). Restart the server to enable " + record.getName() + ".");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Something went wrong downloading " + record.getName() + ".");
                break;
        }
    }
}
