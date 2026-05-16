package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.PluginFolderService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class CleanCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final PluginFolderService pluginFolderService;
    private final Plugin plugin;

    public CleanCommand(EphemeralData ephemeralData, PluginFolderService pluginFolderService, Plugin plugin) {
        super(new ArrayList<>(List.of("clean")), new ArrayList<>(List.of("dpm.clean")));
        this.ephemeralData = ephemeralData;
        this.pluginFolderService = pluginFolderService;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "Scanning for duplicate plugin JARs...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> conflicts = new ArrayList<>();
            for (ProjectRecord record : ephemeralData.getAllProjectRecords()) {
                for (File conflict : pluginFolderService.findConflictingJars(record)) {
                    conflicts.add(conflict.getName() + " (" + record.getName() + ")");
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (conflicts.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "No duplicate JARs found.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Found " + conflicts.size() + " duplicate JAR(s) to remove:");
                    for (String entry : conflicts) {
                        sender.sendMessage(ChatColor.AQUA + "  - " + entry);
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Run " + ChatColor.WHITE + "/dpm clean --confirm" + ChatColor.YELLOW + " to delete them.");
                }
            });
        });
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--confirm")) {
            sender.sendMessage(ChatColor.AQUA + "Removing duplicate plugin JARs...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<String> removed = new ArrayList<>();
                for (ProjectRecord record : ephemeralData.getAllProjectRecords()) {
                    for (File conflict : pluginFolderService.findConflictingJars(record)) {
                        if (conflict.delete()) {
                            removed.add(conflict.getName() + " (" + record.getName() + ")");
                        }
                    }
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (removed.isEmpty()) {
                        sender.sendMessage(ChatColor.GREEN + "No duplicate JARs found.");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Removed " + removed.size() + " duplicate JAR(s):");
                        for (String entry : removed) {
                            sender.sendMessage(ChatColor.AQUA + "  - " + entry);
                        }
                        sender.sendMessage(ChatColor.YELLOW + "Restart the server to apply changes.");
                    }
                });
            });
            return true;
        }
        return execute(sender);
    }
}
