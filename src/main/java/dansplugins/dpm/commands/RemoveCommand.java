package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.DependencyResolutionService;
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
    private final DependencyResolutionService dependencyResolutionService;

    public RemoveCommand(EphemeralData ephemeralData, PluginFolderService pluginFolderService,
                         VersionStore versionStore, DependencyResolutionService dependencyResolutionService) {
        super(new ArrayList<>(List.of("remove")), new ArrayList<>(List.of("dpm.remove")));
        this.ephemeralData = ephemeralData;
        this.pluginFolderService = pluginFolderService;
        this.versionStore = versionStore;
        this.dependencyResolutionService = dependencyResolutionService;
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
            sender.sendMessage(ChatColor.RED + "Plugin not found: " + name + ". Use /dpm search <keyword> to find the right name.");
            return false;
        }
        File jar = pluginFolderService.getInstalledFile(record);
        if (jar == null) {
            sender.sendMessage(ChatColor.YELLOW + record.getName() + " is not installed.");
            return true;
        }

        List<ProjectRecord> installed = pluginFolderService.filterInstalled(ephemeralData.getAllProjectRecords());
        List<String> dependents = dependencyResolutionService.findDependents(record.getName(), installed);

        boolean confirmed = args.length >= 2 && args[1].equalsIgnoreCase("--confirm");
        if (!confirmed) {
            if (!dependents.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Warning: " + formatList(dependents)
                        + " declare a hard dependency on " + record.getName() + ".");
                sender.sendMessage(ChatColor.YELLOW + "Removing " + record.getName()
                        + " may cause those plugins to stop working.");
            }
            sender.sendMessage(ChatColor.YELLOW + "This will delete " + ChatColor.WHITE + jar.getName() + ChatColor.YELLOW + " from the plugins folder.");
            sender.sendMessage(ChatColor.YELLOW + "Run " + ChatColor.WHITE + "/dpm remove " + name + " --confirm" + ChatColor.YELLOW + " to proceed.");
            return true;
        }

        if (!dependents.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Warning: " + formatList(dependents)
                    + " declare a hard dependency on " + record.getName() + " and may stop working.");
        }
        if (jar.delete()) {
            versionStore.removeTag(record.getName());
            sender.sendMessage(ChatColor.GREEN + "Removed " + record.getName() + ".");
            sender.sendMessage(ChatColor.YELLOW + "Restart the server for the removal to take effect.");
            sender.sendMessage(ChatColor.YELLOW + "To reinstall, run " + ChatColor.WHITE + "/dpm get " + record.getName() + ChatColor.YELLOW + ".");
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

    private String formatList(List<String> names) {
        if (names.size() == 1) return names.get(0);
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size() - 1; i++) {
            sb.append(names.get(i)).append(", ");
        }
        sb.append("and ").append(names.get(names.size() - 1));
        return sb.toString();
    }
}
