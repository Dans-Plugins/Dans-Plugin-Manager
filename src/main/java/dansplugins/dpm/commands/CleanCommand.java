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
    public boolean execute(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.AQUA + "Scanning for duplicate plugin JARs...");
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
                    commandSender.sendMessage(ChatColor.GREEN + "No duplicate JARs found.");
                } else {
                    commandSender.sendMessage(ChatColor.GREEN + "Removed " + removed.size() + " duplicate JAR(s):");
                    for (String entry : removed) {
                        commandSender.sendMessage(ChatColor.AQUA + "  - " + entry);
                    }
                    commandSender.sendMessage(ChatColor.YELLOW + "Restart the server to apply changes.");
                }
            });
        });
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        return execute(commandSender);
    }
}
