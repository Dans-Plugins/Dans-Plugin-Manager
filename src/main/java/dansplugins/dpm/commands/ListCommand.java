package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.PluginFolderService;
import dansplugins.dpm.services.VersionStore;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

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
        sender.sendMessage(ChatColor.AQUA + "=== Plugins (" + records.size() + ") ===");
        for (ProjectRecord record : records) {
            boolean installed = pluginFolderService.isInstalled(record);
            if (installed) {
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
        return execute(sender);
    }
}
