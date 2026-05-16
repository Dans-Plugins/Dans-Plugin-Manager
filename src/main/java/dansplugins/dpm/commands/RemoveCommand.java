package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.PluginFolderService;
import dansplugins.dpm.services.VersionStore;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RemoveCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final PluginFolderService pluginFolderService;
    private final VersionStore versionStore;

    public RemoveCommand(EphemeralData ephemeralData, PluginFolderService pluginFolderService,
                         VersionStore versionStore) {
        super(new ArrayList<>(List.of("remove")), new ArrayList<>(List.of("dpm.remove")));
        this.ephemeralData = ephemeralData;
        this.pluginFolderService = pluginFolderService;
        this.versionStore = versionStore;
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /dpm remove <plugin-name> [--confirm]");
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String name = args[0];
        ProjectRecord record = ephemeralData.getProjectRecord(name);
        if (record == null) {
            sender.sendMessage(ChatColor.RED + "Plugin not found: " + name);
            return false;
        }
        File jar = pluginFolderService.getInstalledFile(record);
        if (jar == null) {
            sender.sendMessage(ChatColor.YELLOW + record.getName() + " is not installed.");
            return true;
        }
        boolean confirmed = args.length >= 2 && args[1].equalsIgnoreCase("--confirm");
        if (!confirmed) {
            sender.sendMessage(ChatColor.YELLOW + "This will delete " + ChatColor.WHITE + jar.getName() + ChatColor.YELLOW + " from the plugins folder.");
            sender.sendMessage(ChatColor.YELLOW + "Run " + ChatColor.WHITE + "/dpm remove " + name + " --confirm" + ChatColor.YELLOW + " to proceed.");
            return true;
        }
        if (jar.delete()) {
            versionStore.removeTag(record.getName());
            sender.sendMessage(ChatColor.GREEN + "Removed " + record.getName() + ".");
            sender.sendMessage(ChatColor.YELLOW + "Restart the server for the removal to take effect.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to delete " + jar.getName() + ". Check server file permissions.");
        }
        return true;
    }

    public List<String> getInstalledPluginNames() {
        List<String> names = new ArrayList<>();
        for (ProjectRecord record : pluginFolderService.filterInstalled(ephemeralData.getAllProjectRecords())) {
            names.add(record.getName());
        }
        return names;
    }
}
