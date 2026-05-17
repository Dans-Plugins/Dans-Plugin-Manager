package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.PluginFolderService;
import dansplugins.dpm.services.VersionStore;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final PluginFolderService pluginFolderService;
    private final VersionStore versionStore;

    public ListCommand(EphemeralData ephemeralData, PluginFolderService pluginFolderService,
                       VersionStore versionStore) {
        super(new ArrayList<>(List.of("list")), new ArrayList<>(List.of("dpm.list")));
        this.ephemeralData = ephemeralData;
        this.pluginFolderService = pluginFolderService;
        this.versionStore = versionStore;
    }

    @Override
    public boolean execute(CommandSender sender) {
        List<ProjectRecord> records = ephemeralData.getAllProjectRecords();
        Set<String> installedNames = new HashSet<>();
        for (ProjectRecord r : pluginFolderService.filterInstalled(records)) {
            installedNames.add(r.getName());
        }
        sender.sendMessage(ChatColor.AQUA + "=== Plugins (" + records.size() + ") ===");
        for (ProjectRecord record : records) {
            if (installedNames.contains(record.getName())) {
                String tag = versionStore.getStoredTag(record.getName());
                String version = tag != null ? " " + tag : "";
                sender.sendMessage(ChatColor.GREEN + record.getName() + version);
            } else {
                sender.sendMessage(ChatColor.GRAY + record.getName() + " (not installed)");
            }
        }
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String filter = args[0];
        if (filter.equalsIgnoreCase("installed")) {
            return executeInstalled(sender);
        }
        if (filter.equalsIgnoreCase("available")) {
            return executeAvailable(sender);
        }
        sender.sendMessage(ChatColor.RED + "Unknown filter: " + filter + ". Use 'installed' or 'available'.");
        return false;
    }

    private boolean executeInstalled(CommandSender sender) {
        List<ProjectRecord> installed = pluginFolderService.filterInstalled(ephemeralData.getAllProjectRecords());
        sender.sendMessage(ChatColor.AQUA + "=== Installed Plugins (" + installed.size() + ") ===");
        for (ProjectRecord record : installed) {
            String tag = versionStore.getStoredTag(record.getName());
            String version = tag != null ? " " + tag : "";
            sender.sendMessage(ChatColor.GREEN + record.getName() + version);
        }
        return true;
    }

    private boolean executeAvailable(CommandSender sender) {
        List<ProjectRecord> all = ephemeralData.getAllProjectRecords();
        Set<String> installedNames = new HashSet<>();
        for (ProjectRecord r : pluginFolderService.filterInstalled(all)) {
            installedNames.add(r.getName());
        }
        List<ProjectRecord> available = new ArrayList<>();
        for (ProjectRecord r : all) {
            if (!installedNames.contains(r.getName())) available.add(r);
        }
        sender.sendMessage(ChatColor.AQUA + "=== Available Plugins (" + available.size() + ") ===");
        for (ProjectRecord record : available) {
            String desc = record.getDescription();
            String suffix = desc != null ? ChatColor.DARK_GRAY + " — " + desc : "";
            sender.sendMessage(ChatColor.GRAY + record.getName() + suffix);
        }
        return true;
    }
}
