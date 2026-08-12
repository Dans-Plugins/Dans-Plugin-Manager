package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.ListController;
import dansplugins.dpm.controllers.ListController.ListEntry;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.repositories.ChannelRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class ListCommand extends AbstractPluginCommand {
    private final ListController listController;
    private final ChannelRepository channelRepository;

    public ListCommand(ListController listController, ChannelRepository channelRepository) {
        super(new ArrayList<>(List.of("list")), new ArrayList<>(List.of("dpm.list")));
        this.listController = listController;
        this.channelRepository = channelRepository;
    }

    /** Appends an [experimental] marker to plugins pinned to the experimental channel. */
    private String channelMarker(ProjectRecord record) {
        return channelRepository.getChannel(record.getName()) == ReleaseChannel.EXPERIMENTAL
                ? ChatColor.YELLOW + " [experimental]"
                : "";
    }

    @Override
    public boolean execute(CommandSender sender) {
        List<ListEntry> entries = listController.listAll();
        sender.sendMessage(ChatColor.AQUA + "=== Plugins (" + entries.size() + ") ===");
        for (ListEntry entry : entries) {
            if (entry.isInstalled()) {
                sender.sendMessage(ChatColor.GREEN + entry.getRecord().getName() + versionSuffix(entry) + channelMarker(entry.getRecord()));
            } else {
                sender.sendMessage(ChatColor.GRAY + entry.getRecord().getName() + " (not installed)");
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
        List<ListEntry> entries = listController.listInstalled();
        sender.sendMessage(ChatColor.AQUA + "=== Installed Plugins (" + entries.size() + ") ===");
        for (ListEntry entry : entries) {
            sender.sendMessage(ChatColor.GREEN + entry.getRecord().getName() + versionSuffix(entry) + channelMarker(entry.getRecord()));
        }
        return true;
    }

    private boolean executeAvailable(CommandSender sender) {
        List<ProjectRecord> available = listController.listAvailable();
        sender.sendMessage(ChatColor.AQUA + "=== Available Plugins (" + available.size() + ") ===");
        for (ProjectRecord record : available) {
            String desc = record.getDescription();
            String suffix = desc != null ? ChatColor.DARK_GRAY + " — " + desc : "";
            sender.sendMessage(ChatColor.GRAY + record.getName() + suffix);
        }
        return true;
    }

    private String versionSuffix(ListEntry entry) {
        return entry.getStoredTag() != null ? " " + entry.getStoredTag() : "";
    }
}
