package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.UpdateController;
import dansplugins.dpm.controllers.UpdateController.PluginResult;
import dansplugins.dpm.controllers.UpdateController.SelectionResult;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateCommand extends AbstractPluginCommand {
    private final UpdateController updateController;
    private final Plugin plugin;

    public UpdateCommand(UpdateController updateController, Plugin plugin) {
        super(new ArrayList<>(List.of("update")), new ArrayList<>(List.of("dpm.update")));
        this.updateController = updateController;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender) {
        List<ProjectRecord> installed = updateController.getInstalledPlugins();
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
        SelectionResult selection = updateController.selectForUpdate(names);
        for (String name : selection.getNotFound()) {
            sender.sendMessage(ChatColor.RED + "Plugin not found: " + name + ". Use /dpm search <keyword> to find the right name.");
        }
        for (String name : selection.getNotInstalled()) {
            sender.sendMessage(ChatColor.YELLOW + name + " is not installed — use /dpm get " + name + " first.");
        }
        List<ProjectRecord> toUpdate = selection.getToUpdate();
        if (toUpdate.isEmpty()) return false;
        sender.sendMessage(ChatColor.AQUA + "Checking " + toUpdate.size() + " plugin(s) for updates...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runUpdates(sender, toUpdate));
        return true;
    }

    public List<String> getInstalledPluginNames() {
        return updateController.getInstalledPlugins()
                .stream().map(ProjectRecord::getName).collect(Collectors.toList());
    }

    // A plugin pinned to experimental is skipped by every update run for as long as its repository
    // publishes no main-branch build, so the message has to name the pin and the way out of it —
    // otherwise it reads as "this project has never cut a release", which is a different problem.
    private String noReleaseSuffix(PluginResult result) {
        if (result.getChannel() != ReleaseChannel.EXPERIMENTAL) {
            return " has no published release yet.";
        }
        return " has no experimental build published — it stays pinned to the experimental channel"
                + " and will be skipped until one is published. Use /dpm get " + result.getRecord().getName()
                + " --stable to switch it back to published releases.";
    }

    private void runUpdates(CommandSender sender, List<ProjectRecord> records) {
        List<PluginResult> results = updateController.runBatch(records);
        int updated = 0, upToDate = 0, skipped = 0, failed = 0;
        for (PluginResult result : results) {
            ProjectRecord record = result.getRecord();
            String msg;
            switch (result.getOutcome()) {
                case ALREADY_UP_TO_DATE:
                    upToDate++;
                    String oldTag = result.getOldTag();
                    msg = ChatColor.GREEN + record.getName() + (oldTag != null ? " " + oldTag : "") + " already up to date.";
                    break;
                case NO_RELEASE:
                    skipped++;
                    msg = ChatColor.YELLOW + record.getName() + noReleaseSuffix(result);
                    break;
                case UPDATED:
                    updated++;
                    String versionDiff = UpdateController.versionDiffSuffix(result.getOldTag(), result.getNewTag());
                    msg = ChatColor.GREEN + "Updated " + record.getName() + versionDiff + ".";
                    break;
                case NETWORK_ERROR:
                    failed++;
                    msg = ChatColor.RED + "Failed to update " + record.getName() + " (could not reach GitHub — check console for details).";
                    break;
                case FILE_ERROR:
                    failed++;
                    msg = ChatColor.RED + "Failed to update " + record.getName() + " (could not write to plugins folder — check server file permissions).";
                    break;
                default:
                    failed++;
                    msg = ChatColor.RED + "Failed to update " + record.getName() + ".";
                    break;
            }
            final String fmsg = msg;
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(fmsg));
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
