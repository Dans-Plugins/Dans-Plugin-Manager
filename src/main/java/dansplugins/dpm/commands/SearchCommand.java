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

public class SearchCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final PluginFolderService pluginFolderService;
    private final VersionStore versionStore;

    public SearchCommand(EphemeralData ephemeralData, PluginFolderService pluginFolderService,
                         VersionStore versionStore) {
        super(new ArrayList<>(List.of("search")), new ArrayList<>(List.of("dpm.list")));
        this.ephemeralData = ephemeralData;
        this.pluginFolderService = pluginFolderService;
        this.versionStore = versionStore;
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /dpm search <keyword>");
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String keyword = String.join(" ", args);
        List<ProjectRecord> matches = new ArrayList<>();
        for (ProjectRecord record : ephemeralData.getAllProjectRecords()) {
            if (matchesKeyword(record, keyword)) matches.add(record);
        }
        if (matches.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No plugins found matching \"" + keyword + "\".");
            return true;
        }
        Set<String> installedNames = new HashSet<>();
        for (ProjectRecord r : pluginFolderService.filterInstalled(matches)) {
            installedNames.add(r.getName());
        }
        sender.sendMessage(ChatColor.AQUA + "=== Search Results (" + matches.size() + ") ===");
        for (ProjectRecord record : matches) {
            String desc = record.getDescription() != null ? ChatColor.GRAY + " — " + record.getDescription() : "";
            if (installedNames.contains(record.getName())) {
                String tag = versionStore.getStoredTag(record.getName());
                String version = tag != null ? " " + tag : "";
                sender.sendMessage(ChatColor.GREEN + record.getName() + version + desc);
            } else {
                sender.sendMessage(ChatColor.GRAY + record.getName() + desc);
            }
        }
        return true;
    }

    static boolean matchesKeyword(ProjectRecord record, String keyword) {
        String kw = keyword.toLowerCase();
        if (record.getName().toLowerCase().contains(kw)) return true;
        return record.getDescription() != null
                && record.getDescription().toLowerCase().contains(kw);
    }
}
