package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.ListController;
import dansplugins.dpm.controllers.ListController.ListEntry;
import dansplugins.dpm.controllers.ListController.OutdatedEntry;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.utils.ResultMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class ListCommand extends AbstractPluginCommand {
    private final ListController listController;
    private final ChannelRepository channelRepository;
    private final Plugin plugin;
    private final ResultMessenger messenger;

    public ListCommand(ListController listController, ChannelRepository channelRepository, Plugin plugin) {
        super(new ArrayList<>(List.of("list")), new ArrayList<>(List.of("dpm.list")));
        this.listController = listController;
        this.channelRepository = channelRepository;
        this.plugin = plugin;
        this.messenger = new ResultMessenger(plugin.getLogger());
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
        if (filter.equalsIgnoreCase("outdated")) {
            return executeOutdated(sender);
        }
        sender.sendMessage(ChatColor.RED + "Unknown filter: " + filter + ". Use 'installed', 'available', or 'outdated'.");
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

    private boolean executeOutdated(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "Checking installed plugins for newer builds...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<OutdatedEntry> entries = listController.listOutdated();
            Bukkit.getScheduler().runTask(plugin, () -> showOutdated(sender, entries));
        });
        return true;
    }

    // Only plugins with a newer build are listed; plugins whose status could not be determined are
    // reported after the list so a lookup failure is never mistaken for "up to date".
    private void showOutdated(CommandSender sender, List<OutdatedEntry> entries) {
        List<OutdatedEntry> outdated = new ArrayList<>();
        List<OutdatedEntry> unknown = new ArrayList<>();
        for (OutdatedEntry entry : entries) {
            switch (entry.getStaleness()) {
                case OUTDATED: outdated.add(entry); break;
                case NO_RELEASE:
                case LOOKUP_FAILED: unknown.add(entry); break;
                default: break;
            }
        }
        messenger.send(sender, ChatColor.AQUA + "=== Outdated Plugins (" + outdated.size() + ") ===");
        for (OutdatedEntry entry : outdated) {
            String from = entry.getStoredTag() != null ? entry.getStoredTag() : "unknown version";
            String to = entry.getLatestTag() != null ? entry.getLatestTag() : "unknown version";
            messenger.send(sender, ChatColor.YELLOW + entry.getRecord().getName() + " " + from + " → " + to
                    + channelMarker(entry.getRecord()));
        }
        for (OutdatedEntry entry : unknown) {
            String reason = entry.getStaleness() == ListController.Staleness.NO_RELEASE
                    ? (entry.getChannel() == ReleaseChannel.EXPERIMENTAL ? "no experimental build published" : "no published release")
                    : "could not reach GitHub — check console for details";
            messenger.send(sender, ChatColor.GRAY + entry.getRecord().getName() + " (" + reason + ")");
        }
        if (outdated.isEmpty() && unknown.isEmpty()) {
            messenger.send(sender, ChatColor.GREEN + (entries.isEmpty()
                    ? "No managed plugins are currently installed."
                    : "All installed plugins are up to date."));
        } else if (!outdated.isEmpty()) {
            messenger.send(sender, ChatColor.AQUA + "Run /dpm update to install the newer builds.");
        }
    }

    private String versionSuffix(ListEntry entry) {
        return entry.getStoredTag() != null ? " " + entry.getStoredTag() : "";
    }
}
